package uz.nextqadam.bot.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // TODO: har bir handler metodida uz.nextqadam.bot.common.errorlog.ErrorNotificationService.logAndNotify(...)
    // chaqirilishi va logId javobga qo'shilishi kerak

    @ExceptionHandler(NextQadamException.class)
    public ResponseEntity<String> handleNextQadamException(NextQadamException e) {
        // TODO: implementatsiya
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleException(Exception e) {
        // TODO: implementatsiya
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
