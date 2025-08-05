package com.chatapp.dto;

import lombok.Data;

@Data
public class MessageEditRequest {
    private String content;

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}
    
} 