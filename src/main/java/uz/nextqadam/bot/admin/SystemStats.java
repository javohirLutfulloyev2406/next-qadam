package uz.nextqadam.bot.admin;

public record SystemStats(int totalUsers, int newUsersToday, int activeUsersToday, int totalActiveGoals,
                           int tasksDoneToday, int errorsLast24h) {
}
