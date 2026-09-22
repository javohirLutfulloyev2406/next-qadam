package uz.nextqadam.bot.ai;

public class AiClientException extends RuntimeException {

    private final boolean transientFailure;

    public AiClientException(String message, Throwable cause) {
        this(message, cause, false);
    }

    /**
     * transientFailure=true — xato AI provayderning vaqtinchalik band bo'lishi (503/502/429 yoki
     * timeout) tufayli bo'lgan va retry'lar ham tugagan holatni bildiradi. Chaqiruvchi
     * (masalan GoalServiceImpl) shu bayroq orqali foydalanuvchiga aniqroq signal berishi mumkin
     * ("hozircha band, birozdan so'ng qayta urinib ko'ring").
     */
    public AiClientException(String message, Throwable cause, boolean transientFailure) {
        super(message, cause);
        this.transientFailure = transientFailure;
    }

    public boolean isTransientFailure() {
        return transientFailure;
    }
}