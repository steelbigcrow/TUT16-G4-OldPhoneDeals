package com.oldphonedeals.dto.response.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderExportResult {
    private String fileName;
    private String contentType;
    private byte[] content;
}
