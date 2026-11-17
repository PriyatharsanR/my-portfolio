package com.backend.portfolio.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

@Entity
@Table(name = "contact_messages")
@Getter
@Setter
@NoArgsConstructor
public class ContactMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 120)
    @Column(nullable = false, length = 120)
    private String senderName;

    @NotBlank
    @Email
    @Size(max = 254)
    @Column(nullable = false, length = 254)
    private String senderEmail;

    @NotBlank
    @Size(max = 5000)
    @Column(nullable = false, length = 5000)
    private String body;

    @NotNull
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 16)
    private Status status = Status.NEW;

    @Column(nullable = false, updatable = false)
    private Instant submittedAt;

    @Version
    @Column(nullable = false)
    private Long version;

    @PrePersist
    void assignSubmissionTime() {
        if (submittedAt == null) {
            submittedAt = Instant.now();
        }
    }

    public enum Status {
        NEW, READ, ARCHIVED
    }
}
