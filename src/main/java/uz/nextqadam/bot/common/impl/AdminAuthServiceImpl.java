package uz.nextqadam.bot.common.impl;

import org.springframework.stereotype.Service;

import uz.nextqadam.bot.common.AdminAuthService;
import uz.nextqadam.bot.common.AdminProperties;

@Service
public class AdminAuthServiceImpl implements AdminAuthService {

    private final AdminProperties adminProperties;

    public AdminAuthServiceImpl(AdminProperties adminProperties) {
        this.adminProperties = adminProperties;
    }

    @Override
    public boolean isAdmin(Long telegramId) {
        return telegramId != null && adminProperties.getAdminTelegramIds().contains(telegramId);
    }
}
