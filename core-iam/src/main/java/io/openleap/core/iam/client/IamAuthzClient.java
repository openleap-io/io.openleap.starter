package io.openleap.core.iam.client;

import io.openleap.core.iam.dto.CheckPermissionRequest;
import io.openleap.core.iam.dto.CheckPermissionResponse;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

@HttpExchange("/api/v1/iam/authz")
public interface IamAuthzClient {

    @PostExchange("/check")
    CheckPermissionResponse check(@RequestBody CheckPermissionRequest request);

}