package it.gls.dipendenti.access.service;

import it.gls.dipendenti.access.dto.CardWithDetails;
import it.gls.dipendenti.access.exception.CardAlreadyExistException;
import it.gls.dipendenti.access.exception.CardNotFoundException;
import it.gls.dipendenti.access.model.Card;
import it.gls.dipendenti.access.model.CardAssignment;
import it.gls.dipendenti.access.repository.CardAssignmentRepository;
import it.gls.dipendenti.access.repository.CardRepository;
import it.gls.dipendenti.hr.model.Employee;
import it.gls.dipendenti.hr.repository.EmployeeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class CardService {
    private final CardRepository cardRepository;
    private final CardAssignmentRepository assignmentRepository;
    private final EmployeeRepository employeeRepository;

    public CardService(CardRepository cardRepository, CardAssignmentRepository assignmentRepository, EmployeeRepository employeeRepository) {
        this.cardRepository = cardRepository;
        this.employeeRepository = employeeRepository;
        this.assignmentRepository = assignmentRepository;
    }

    public boolean cardExistsByUid(String uid) {
        return cardRepository.getCardByUid(uid).isPresent();
    }

    public boolean cardExistsById(Long id) {
        return cardRepository.getCardById(id).isPresent();
    }

    @Transactional
    public Card createCard(Card card) {
        if (card.uid() == null)
            throw new IllegalArgumentException("Card uid cannot be null");
        if (cardExistsByUid(card.uid()))
            throw new CardAlreadyExistException(card.uid());
        return cardRepository.save(
                // When adding a card it cannot be deleted
                new Card(null, card.uid(), false)
        );
    }

    public List<CardWithDetails> getAssignedCardsWithDetails() {
        List<Card> cards = cardRepository.getAllCards();
        List<CardWithDetails> output = new ArrayList<>();
        for (Card c : cards) {
            Long employeeId = assignmentRepository.getAssignedEmployeeId(c.id());
            if (employeeId != null) {
                Employee assingedEmployee = employeeRepository.findById(employeeId).orElse(null);
                CardAssignment lastAssignment = assignmentRepository.getActiveAssignmentByCard(c.id()).orElse(null);
                if (lastAssignment != null) {
                    if (assingedEmployee != null) {
                        output.add(new CardWithDetails(c.id(), c.uid(), employeeId, assingedEmployee.name(), assingedEmployee.surname(), lastAssignment.id(), lastAssignment.startDate(), lastAssignment.endDate(), lastAssignment.endDate() == null));
                    } else {
                        output.add(new CardWithDetails(c.id(), c.uid(), employeeId, "NOT FOUND", "", lastAssignment.id(), lastAssignment.startDate(), lastAssignment.endDate(), lastAssignment.endDate() == null));
                    }
                }
            }
        }
        return output;
    }

    @Transactional
    public void deleteCard(Long cardId) {
        if (cardId == null)
            throw new IllegalArgumentException("Card id is null");
        if(!cardRepository.deleteCard(cardId))
            throw new CardNotFoundException();
    }

    public void restoreCard(Long cardId) {
        if (cardId == null)
            throw new IllegalArgumentException("Card id is null");
        if(!cardRepository.restoreCard(cardId))
            throw new CardNotFoundException();
    }

    public Long getCardCount() {
        return cardRepository.getCardCount();
    }

    public Card getById(Long id) {
        return cardRepository.getCardById(id).orElseThrow(CardNotFoundException::new);
    }

    public Card getByUid(String uid) {
        return cardRepository.getCardByUid(uid).orElse(null);
    }

    public List<Card> getAll() {
        return cardRepository.getAllCards();
    }

    public List<Card> getAllDeleted() {
        return  cardRepository.getDeletedCards();
    }

    public List<Card> getUnassignedCards() {
        return cardRepository.getUnassignedCards();
    }




}
