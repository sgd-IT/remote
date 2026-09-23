package com.enjoyiot.module.eiot.controller.admin.rule.vo;




import com.enjoyiot.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "RuleLogPageReq")
@Data


public class RuleLogPageReq extends PageParam {

	private static final long serialVersionUID = -1L;

	@Schema(description="时间")
	private Long time;

	@Schema(description="规则id")
	private Long ruleId;

	@Schema(description="状态")
	private String state1;

	@Schema(description="内容")
	private String content;

	@Schema(description="是否成功")
	private Boolean success;

    }
