package io.openleap.common.iam;

import io.openleap.common.iam.dto.CheckPermissionRequestDto;
import io.openleap.common.iam.dto.CheckPermissionResponseDto;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

@HttpExchange("/api/v1/iam/authz")
public interface IamAuthzClient {

    @PostExchange("/check")
    CheckPermissionResponseDto check(@RequestBody CheckPermissionRequestDto request);
}
