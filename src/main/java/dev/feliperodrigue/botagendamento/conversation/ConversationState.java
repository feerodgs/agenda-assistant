package dev.feliperodrigue.botagendamento.conversation;

public class ConversationState {

    public enum Step {
        WAITING_DATE,
        WAITING_TIME,
        WAITING_DESCRIPTION,
        WAITING_CONFLICT_CONFIRMATION
    }

    private Step step = Step.WAITING_DATE;
    private String date;
    private String time;
    private String description;

    public Step getStep() { return step; }
    public void setStep(Step step) { this.step = step; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
