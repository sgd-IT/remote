package com.enjoyiot.eiot.temporal.es.document;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.dromara.easyes.annotation.IndexName;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(indexName = "thing_model_message")
@IndexName("thing_model_message")
public class DocThingModelMessage {

    @Id
    private String id;

    private String mid;

    private String deviceId;

    private String productKey;

    private String deviceName;

    private String type;

    private String identifier;

    private int code;

    private Object data;

    @Field(type = FieldType.Date)
    private Long occurred;

    @Field(type = FieldType.Date)
    private Long time;

}
