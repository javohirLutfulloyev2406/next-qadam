package uz.nextqadam.bot.common;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@MappedSuperclass
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public abstract class BaseEntity {

    @Id
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    /**
     * Deyarli barcha domain entity'larda (User'dan tashqari) @SQLRestriction("deleted = false")
     * bor — shu sababli oddiy so'rovlar bu maydon true bo'lgan qatorlarni avtomatik chetlab o'tadi.
     * Agar ANIQ shu qatorlarni (masalan admin panelida "o'chirilganlar" statistikasi uchun) ko'rish
     * kerak bo'lsa, @SQLRestriction'ni HQL/JPQL orqali chetlab o'tib bo'lmaydi — buning o'rniga
     * native SQL so'rov (nativeQuery=true) yozish yoki Hibernate Session'da alohida @Filter
     * mexanizmini qo'llash kerak bo'ladi (ResetServiceImpl'dagi hardDelete metodlari aynan shu
     * sababli native query ishlatadi).
     */
    @Builder.Default
    @Column(name = "deleted", nullable = false)
    private boolean deleted = false;
}
