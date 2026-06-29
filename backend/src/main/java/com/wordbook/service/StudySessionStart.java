package com.wordbook.service;

import com.wordbook.domain.Card;
import com.wordbook.domain.StudySession;
import java.util.List;

/** Result of starting a study session: the persisted session and the ordered (shuffled) cards. */
public record StudySessionStart(StudySession session, List<Card> cards) {}
