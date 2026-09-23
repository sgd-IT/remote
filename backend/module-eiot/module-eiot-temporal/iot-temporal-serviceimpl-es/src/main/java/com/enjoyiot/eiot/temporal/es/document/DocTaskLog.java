package com.enjoyiot.eiot.temporal.es.document;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(indexName = "task_log")
public class DocTaskLog {

    @Id
    private String id;

    private Long taskId;

    private String content;

    private Boolean success;

    @Field(type = FieldType.Date)
    private Long logAt;
}
