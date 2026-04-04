package io.openleap.core.scheduling.web.controller;

import io.openleap.core.scheduling.support.TestHandler;
import io.openleap.core.scheduling.support.TestIdentityFilter;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@ActiveProfiles("test")
@TestPropertySource(properties = "task.executor=in-memory")
@Import({TestIdentityFilter.class, TestHandler.class})
class TaskInMemoryControllerIT extends AbstractTaskControllerIT {

    // TODO (itaseski): There is a race condition in integration test canceling an in memory tasks since it
    //  completes before we manage to call the /cancel endpoint. We will need to introduce a slow handler that
    //  will Thread.sleep() and give us some time to cancel it.

}
