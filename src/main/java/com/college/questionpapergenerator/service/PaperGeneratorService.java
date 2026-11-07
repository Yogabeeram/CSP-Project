package com.college.questionpapergenerator.service;

import com.college.questionpapergenerator.dto.GeneratedPaperResponse;
import com.college.questionpapergenerator.dto.BloomDistributionReport;
import com.college.questionpapergenerator.dto.PaperGenerationRequest;
import com.college.questionpapergenerator.dto.PaperSectionRequirement;
import com.college.questionpapergenerator.entity.BloomLevel;
import com.college.questionpapergenerator.entity.Difficulty;
import com.college.questionpapergenerator.entity.ExamPaper;
import com.college.questionpapergenerator.entity.PaperQuestion;
import com.college.questionpapergenerator.entity.Question;
import com.college.questionpapergenerator.entity.Subject;
import com.college.questionpapergenerator.exception.PaperGenerationException;
import com.college.questionpapergenerator.exception.ResourceNotFoundException;
import com.college.questionpapergenerator.repository.ExamPaperRepository;
import com.college.questionpapergenerator.repository.QuestionRepository;
import com.college.questionpapergenerator.repository.SubjectRepository;
import com.college.questionpapergenerator.util.PaperSelectionUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class PaperGeneratorService {

    private final QuestionRepository questionRepository;
    private final SubjectRepository subjectRepository;
    private final ExamPaperRepository examPaperRepository;
    private final BloomDistributionService bloomDistributionService;

    public PaperGeneratorService(
            QuestionRepository questionRepository,
            SubjectRepository subjectRepository,
            ExamPaperRepository examPaperRepository,
            BloomDistributionService bloomDistributionService) {
        this.questionRepository = questionRepository;
        this.subjectRepository = subjectRepository;
        this.examPaperRepository = examPaperRepository;
        this.bloomDistributionService = bloomDistributionService;
    }

    @Transactional
    public GeneratedPaperResponse generate(PaperGenerationRequest request) {
        validateRequest(request);
        Subject subject = subjectRepository.findById(request.subjectId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Subject " + request.subjectId() + " was not found"));

        List<Integer> units = request.selectedUnits().stream().distinct().sorted().toList();
        List<Question> candidates = questionRepository
                .findBySubject_IdAndApprovedTrueAndTopic_UnitNumberIn(request.subjectId(), units);
        if (candidates.size() < request.totalQuestions()) {
            throw new PaperGenerationException(
                    "Insufficient approved questions in the selected subject and units: need "
                            + request.totalQuestions() + ", found " + candidates.size());
        }

        Map<Difficulty, Integer> difficultyTargets = PaperSelectionUtil.allocatePercentages(
                request.difficultyDistribution(), List.of(Difficulty.values()), request.totalQuestions());
        Map<BloomLevel, Integer> bloomTargets = bloomDistributionService.calculateRequiredCounts(
                request.bloomDistribution(), request.totalQuestions(), candidates);
        Map<Integer, Integer> unitMarkTargets = calculateUnitMarkTargets(request, units);
        List<PaperSelectionUtil.Slot> slots = createSlots(request);

        List<PaperSelectionUtil.Selection> selections = PaperSelectionUtil.select(
                candidates,
                request.totalMarks(),
                request.totalQuestions(),
                difficultyTargets,
                bloomTargets,
                unitMarkTargets,
                slots
        );
        List<Question> selectedQuestions = selections.stream()
                .map(PaperSelectionUtil.Selection::question)
                .toList();
        BloomDistributionReport bloomReport = bloomDistributionService.validateAndReport(
                request.bloomDistribution(), bloomTargets, selectedQuestions);

        ExamPaper paper = new ExamPaper(
                request.examTitle().trim(),
                subject,
                request.totalMarks(),
                request.totalQuestions(),
                request.duration(),
                request.examDate(),
                request.examType()
        );
        List<PaperQuestion> paperQuestions = new ArrayList<>();
        for (int index = 0; index < selections.size(); index++) {
            PaperSelectionUtil.Selection selection = selections.get(index);
            paperQuestions.add(new PaperQuestion(
                    paper,
                    selection.question(),
                    index + 1,
                    selection.sectionName(),
                    selection.question().getMarks()
            ));
        }
        paper.setPaperQuestions(paperQuestions);
        ExamPaper savedPaper = examPaperRepository.save(paper);
        return toResponse(savedPaper, paperQuestions, bloomReport);
    }

    private void validateRequest(PaperGenerationRequest request) {
        Set<Integer> selectedUnits = new HashSet<>(request.selectedUnits());
        if (selectedUnits.size() != request.selectedUnits().size()) {
            throw new PaperGenerationException("Selected units must not contain duplicates");
        }
        if (request.questionMarks() != null && request.sections() != null && !request.sections().isEmpty()) {
            throw new PaperGenerationException("Use either questionMarks or sections, not both");
        }
        List<PaperSelectionUtil.Slot> slots = createSlots(request);
        if (!slots.isEmpty()) {
            if (slots.size() != request.totalQuestions()) {
                throw new PaperGenerationException(
                        "Question marks or section slots must equal totalQuestions");
            }
            long specifiedMarks = slots.stream()
                    .map(PaperSelectionUtil.Slot::requiredMarks)
                    .mapToLong(Integer::longValue)
                    .sum();
            if (specifiedMarks != request.totalMarks()) {
                throw new PaperGenerationException(
                        "Sum of question or section marks must equal totalMarks");
            }
        }
        if (request.unitWeightage() != null && !request.unitWeightage().isEmpty()) {
            if (!request.unitWeightage().keySet().equals(selectedUnits)) {
                throw new PaperGenerationException(
                        "Unit weightage must specify every selected unit and no unselected units");
            }
        }
    }

    private List<PaperSelectionUtil.Slot> createSlots(PaperGenerationRequest request) {
        List<PaperSelectionUtil.Slot> slots = new ArrayList<>();
        if (request.sections() != null && !request.sections().isEmpty()) {
            for (PaperSectionRequirement section : request.sections()) {
                for (Integer marks : section.questionMarks()) {
                    slots.add(new PaperSelectionUtil.Slot(section.sectionName().trim(), marks));
                }
            }
        } else if (request.questionMarks() != null && !request.questionMarks().isEmpty()) {
            for (Integer marks : request.questionMarks()) {
                slots.add(new PaperSelectionUtil.Slot("Section A", marks));
            }
        }
        return slots;
    }

    private Map<Integer, Integer> calculateUnitMarkTargets(
            PaperGenerationRequest request,
            List<Integer> units) {
        if (request.unitWeightage() == null || request.unitWeightage().isEmpty()) {
            return Map.of();
        }
        return PaperSelectionUtil.allocatePercentages(
                request.unitWeightage(),
                units,
                request.totalMarks()
        );
    }

    private GeneratedPaperResponse toResponse(
            ExamPaper paper,
            List<PaperQuestion> paperQuestions,
            BloomDistributionReport bloomReport) {
        List<GeneratedPaperResponse.GeneratedPaperQuestion> questions = paperQuestions.stream()
                .map(paperQuestion -> new GeneratedPaperResponse.GeneratedPaperQuestion(
                        paperQuestion.getQuestionNumber(),
                        paperQuestion.getSectionName(),
                        paperQuestion.getQuestion().getId(),
                        paperQuestion.getQuestion().getQuestionText(),
                        paperQuestion.getMarks(),
                        paperQuestion.getQuestion().getDifficulty(),
                        paperQuestion.getQuestion().getBloomLevel(),
                        paperQuestion.getQuestion().getTopic().getUnitNumber()
                ))
                .toList();
        return new GeneratedPaperResponse(
                paper.getId(),
                paper.getExamTitle(),
                paper.getExamType(),
                paper.getSubject().getId(),
                paper.getTotalMarks(),
                paper.getTotalQuestions(),
                paper.getDuration(),
                paper.getExamDate(),
                paper.getCreatedAt(),
                bloomReport,
                questions
        );
    }
}
