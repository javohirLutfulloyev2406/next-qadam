package uz.nextqadam.bot.common;

/**
 * TelegramExecutor barcha xabarlarni parseMode=HTML bilan yuboradi (masalan &lt;b&gt;, &lt;s&gt;
 * teglarini to'g'ri ko'rsatish uchun). AI'dan kelgan erkin matn "&lt;", "&amp;" kabi belgilarni o'z
 * ichiga olishi mumkin — bunday matnni escape qilmasdan yuborish Telegram'ning butun xabarni
 * "can't parse entities" xatosi bilan rad etishiga olib kelishi mumkin.
 */
public final class HtmlEscaper {

    private HtmlEscaper() {
    }

    public static String escape(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
