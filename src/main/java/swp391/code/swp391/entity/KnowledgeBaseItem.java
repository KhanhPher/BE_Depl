package swp391.code.swp391.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "knowledge_base")
@Data // (Dùng Lombok cho nhanh)
public class KnowledgeBaseItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(columnDefinition = "TEXT")
    private String question;

    @Column(columnDefinition = "TEXT")
    private String answer;
}