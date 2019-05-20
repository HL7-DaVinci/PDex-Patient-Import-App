package org.hl7.davinci.pdex.refimpl.s2.cdshooks.model;

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

  public Suggestion addActionsItem(Action actionsItem) {
    this.actions.add(actionsItem);
    return this;
  }

}
