package com.chatapp.dto;

import lombok.Data;

@Data
public class MessageForwardRequest {
    private Long targetChatId;

	public Long getTargetChatId() {
		return targetChatId;
	}

	public void setTargetChatId(Long targetChatId) {
		this.targetChatId = targetChatId;
	}
    
} 