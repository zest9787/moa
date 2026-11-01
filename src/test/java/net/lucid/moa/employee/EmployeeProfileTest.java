package net.lucid.moa.employee;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import net.lucid.moa.employee.controller.EmployeeController;
import net.lucid.moa.employee.mock.MockEmployeeRepositoryImpl;
import net.lucid.moa.employee.mock.MockEmployeeExceptionHandler;
import net.lucid.moa.employee.service.impl.EmployeeServiceImpl;
import static org.assertj.core.api.Assertions.assertThat;

class EmployeeProfileTest {
    @Test
    void mockComponentsAreAbsentInProdEvenIfMockIsAlsoRequested() {
        for (String profiles : new String[]{"prod", "prod,mock", "dev", "local"}) {
            new ApplicationContextRunner().withPropertyValues("spring.profiles.active=" + profiles)
                    .withUserConfiguration(EmployeeController.class, EmployeeServiceImpl.class,
                            MockEmployeeRepositoryImpl.class, MockEmployeeExceptionHandler.class)
                    .run(context -> {
                        assertThat(context).hasNotFailed();
                        assertThat(context).doesNotHaveBean(EmployeeController.class);
                        assertThat(context).doesNotHaveBean(EmployeeServiceImpl.class);
                        assertThat(context).doesNotHaveBean(MockEmployeeRepositoryImpl.class);
                        assertThat(context).doesNotHaveBean(MockEmployeeExceptionHandler.class);
                    });
        }
    }
}
