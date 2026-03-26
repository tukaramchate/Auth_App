package com.validation.auth.backend.dtos;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OAuthProviderLinkDto {

    private String provider;
    private String providerEmail;
    private String providerName;
    private String providerImage;
    private boolean isPrimary;
    private String linkedAt;
}
