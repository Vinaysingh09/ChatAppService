package com.chatapp.dto;

import com.chatapp.model.Reaction.ReactionType;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Setter
@Getter
public class ReactionRequest {
    private ReactionType type;

	public ReactionType getType() {
		return type;
	}

	public void setType(ReactionType type) {
		this.type = type;
	}
    
    
} 