package com.balaji.kafkaeventlab.event;

import jakarta.validation.constraints.NotBlank;

public record ChangeOrderStatusCommand(@NotBlank String status) {
}
