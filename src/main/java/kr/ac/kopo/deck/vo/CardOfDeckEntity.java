package kr.ac.kopo.deck.vo;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@ToString
@NoArgsConstructor
@AllArgsConstructor
@Table(name="d_card")
@Entity
@Data
public class CardOfDeckEntity {

    @EmbeddedId
    private CardOfDeckId id;

    @Column
    private int quantity;
}