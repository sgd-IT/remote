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
@Document(indexName = "device_property")
public class DocDeviceProperty {

    @Id
    private String id;

    private String deviceId;

    private String name;

    private Object value;

    @Field(type = FieldType.Date)
    private Long time;

}
