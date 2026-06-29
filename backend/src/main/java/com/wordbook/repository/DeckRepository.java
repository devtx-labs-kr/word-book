package com.wordbook.repository;

import com.wordbook.domain.Deck;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repository for {@link Deck}. */
public interface DeckRepository extends JpaRepository<Deck, UUID> {

  boolean existsByName(String name);

  /** Case-insensitive name substring search (SR-1, business-logic-model §2.1). */
  List<Deck> findByNameContainingIgnoreCase(String name);

  /** Case-insensitive name substring search with a DB-side sort (SR-1/SR-2). */
  List<Deck> findByNameContainingIgnoreCase(String name, Sort sort);
}
