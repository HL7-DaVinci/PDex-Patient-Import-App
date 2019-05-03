package com.healthlx.demo.pdex2019.cdshooks.model;

import lombok.Getter;
import lombok.Setter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class Suggestion {

  private String label;
  private UUID uuid;
  private List<Action> actions = new ArrayList<>();

  /**
   * Add an action.
   *
   * @param actionsItem The action.
   * @return Suggestion
   */
  public Suggestion addActionsItem(Action actionsItem) {
    this.actions.add(actionsItem);
    return this;
  }

}
