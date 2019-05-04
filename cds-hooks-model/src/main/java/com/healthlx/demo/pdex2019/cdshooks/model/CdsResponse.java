package com.healthlx.demo.pdex2019.cdshooks.model;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class CdsResponse {

  /**
   * An array of Cards. Cards can provide a combination of information (for reading), suggested actions (to be applied
   * if a user selects them), and links (to launch an app if the user selects them). The EHR decides how to display
   * cards, but we recommend displaying suggestions using buttons, and links using underlined text. REQUIRED
   */
  private List<Card> cards = new ArrayList<>();

  public CdsResponse addCard(Card cardsItem) {
    this.cards.add(cardsItem);
    return this;
  }
}
