package kr.ac.kopo.deck.vo;

import java.io.Serializable;
import java.util.Objects;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
public class CardOfDeckId implements Serializable {

    private static final long serialVersionUID = 1L;

    @Column(name = "deck_id")
    private int deckId;

    @Column(name = "card_id")
    private int cardId;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CardOfDeckId that = (CardOfDeckId) o;
        return deckId == that.deckId && cardId == that.cardId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(deckId, cardId);
    }
}