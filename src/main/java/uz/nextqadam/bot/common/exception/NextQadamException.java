package uz.nextqadam.bot.common.exception;

public class NextQadamException extends RuntimeException {

    public NextQadamException(String message) {
        super(message);
    }

    public NextQadamException(String message, Throwable cause) {
        super(message, cause);
    }
}
