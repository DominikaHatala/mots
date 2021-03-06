package org.motechproject.mots.domain;

import java.util.ArrayList;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.Table;
import javax.validation.Valid;
import lombok.Getter;
import org.motechproject.mots.domain.enums.CallFlowElementType;
import org.motechproject.mots.validate.CourseReleaseCheck;
import org.motechproject.mots.validate.annotations.IsCorrect;

@Entity
@Table(name = "multiple_choice_question")
@PrimaryKeyJoinColumn(name = "call_flow_element_id")
public class MultipleChoiceQuestion extends CallFlowElement {

  @OneToMany(
      cascade = CascadeType.ALL,
      orphanRemoval = true,
      fetch = FetchType.EAGER)
  @JoinColumn(name = "question_id")
  @OrderBy("choice_id ASC")
  @Getter
  @IsCorrect(groups = {CourseReleaseCheck.class})
  @Valid
  private List<Choice> choices;

  public MultipleChoiceQuestion() {
    super(CallFlowElementType.QUESTION);
  }

  private MultipleChoiceQuestion(String ivrId, String ivrName, String name, String content,
      CallFlowElementType type, Integer listOrder, List<Choice> choices) {
    super(ivrId, ivrName, name, content, type, listOrder);
    this.choices = choices;
  }

  /**
   * Update list content.
   * @param choices list of new Choices
   */
  public void setChoices(List<Choice> choices) {
    if (this.choices == null) {
      this.choices = choices;
    } else if (!this.choices.equals(choices)) {
      this.choices.clear();

      if (choices != null) {
        this.choices.addAll(choices);
      }
    }
  }

  /**
   * Create a copy of Multiple Choice Question.
   * @return copied question
   */
  public MultipleChoiceQuestion copyAsNewDraft() {
    List<Choice> choicesCopy = new ArrayList<>();

    if (choices != null) {
      choices.forEach(choice -> choicesCopy.add(choice.copyAsNewDraft()));
    }

    return new MultipleChoiceQuestion(getIvrId(), getIvrName(), getName(),
        getContent(), getType(), getListOrder(), choicesCopy);
  }
}
