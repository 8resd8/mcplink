package kr.co.mcplink.domain.schedule.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import kr.co.mcplink.global.annotation.AutoSequence;
import kr.co.mcplink.global.common.Constants;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AutoSequence(collection = Constants.COLLECTION_GEMINI_PENDING_QUEUE)
@Document(collection = Constants.COLLECTION_GEMINI_PENDING_QUEUE)
public class GeminiPendingQueue {

    @Id
    private String id;

    @Indexed
    private Long seq;

    @CreatedDate
    @JsonFormat(shape = JsonFormat.Shape.STRING,
            pattern = "yyyy-MM-dd HH:mm:ss",
            timezone = "Asia/Seoul")
    private Instant enqueuedAt;

    @JsonFormat(shape = JsonFormat.Shape.STRING,
            pattern = "yyyy-MM-dd HH:mm:ss",
            timezone = "Asia/Seoul")
    private Instant completedAt;

    @Indexed(unique = true)
    private Long serverId;
    private String resizedReadme;
}