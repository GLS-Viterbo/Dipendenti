package it.gls.dipendenti.access.controller;

import it.gls.dipendenti.access.dto.CardWithDetails;
import it.gls.dipendenti.access.exception.CardAlreadyExistException;
import it.gls.dipendenti.access.exception.CardNotFoundException;
import it.gls.dipendenti.access.model.Card;
import it.gls.dipendenti.access.service.CardService;
import it.gls.dipendenti.util.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cards")
public class CardController {

    private final CardService cardService;

    public CardController(CardService cardService) {
        this.cardService = cardService;
    }

    @PostMapping
    public ResponseEntity<Card> createCard(@RequestBody Card card) {
        Card createdCard = cardService.createCard(card);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdCard);
    }

    @PostMapping("restore/{id}")
    public ResponseEntity<Void> restoreCard(@PathVariable Long id) {
        cardService.restoreCard(id);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCard(@PathVariable Long id) {
        cardService.deleteCard(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<List<Card>> getAllCards() {
        return ResponseEntity.ok(cardService.getAll());
    }

    @GetMapping("/detailed")
    public ResponseEntity<List<CardWithDetails>> getDetailedCardAssignments() {
        return ResponseEntity.ok(cardService.getAssignedCardsWithDetails());
    }

    @GetMapping("/count")
    public ResponseEntity<CardCount> getNotDeletedCardCount() {
        return ResponseEntity.ok(new CardCount(cardService.getCardCount()));
    }

    @GetMapping("/deleted")
    public ResponseEntity<List<Card>> getAllDeletedCards() {
        return ResponseEntity.ok(cardService.getAllDeleted());
    }

    @GetMapping("/unassigned")
    public ResponseEntity<List<Card>> getUnassignedCards() {
        return ResponseEntity.ok(cardService.getUnassignedCards());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Card> getById(@PathVariable Long id) {
        return ResponseEntity.ok(cardService.getById(id));
    }


    @ExceptionHandler(CardAlreadyExistException.class)
    public ResponseEntity<ErrorResponse> handleCardAlreadyExist(CardAlreadyExistException ex) {
        ErrorResponse error = new ErrorResponse(ex.getMessage(), HttpStatus.CONFLICT.value());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    @ExceptionHandler(CardNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleCardNotFound(CardNotFoundException ex) {
        ErrorResponse error = new ErrorResponse(ex.getMessage(), HttpStatus.NOT_FOUND.value());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        ErrorResponse error = new ErrorResponse(ex.getMessage(), HttpStatus.BAD_REQUEST.value());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    public record CardCount(Long count){}




}
