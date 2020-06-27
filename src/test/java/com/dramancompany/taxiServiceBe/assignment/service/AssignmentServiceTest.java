package com.dramancompany.taxiServiceBe.assignment.service;

import com.dramancompany.taxiServiceBe.ServiceTest;
import com.dramancompany.taxiServiceBe.assignment.domain.Assignment;
import com.dramancompany.taxiServiceBe.assignment.dto.AssignmentDto;
import com.dramancompany.taxiServiceBe.assignment.repository.AssignmentRepository;
import com.dramancompany.taxiServiceBe.user.domain.User;
import com.dramancompany.taxiServiceBe.user.repository.UserRepository;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.TemporalUnit;
import java.util.List;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@Transactional(propagation = Propagation.NOT_SUPPORTED)
public class AssignmentServiceTest extends ServiceTest {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private AssignmentRepository assignmentRepository;

    private AssignmentService assignmentService;

    private Assignment waitingAssignment;
    private Assignment completedAssignment;
    private User passenger;
    private User passenger2;
    private User driver;

    @Before
    public void setUp() {
        assignmentRepository.deleteAll();

        assignmentService = new AssignmentService(assignmentRepository);

        passenger = userRepository.save(User.builder()
                .username("je.chang@gmail.com")
                .password("1234")
                .userType(User.UserType.Passenger)
                .build());

        passenger2 = userRepository.save(User.builder()
                .username("je.chang33@gmail.com")
                .password("1234")
                .userType(User.UserType.Passenger)
                .build());

        driver = userRepository.save(User.builder()
                .username("je.chang2@gmail.com")
                .password("1234")
                .userType(User.UserType.Driver)
                .build());

        waitingAssignment = assignmentRepository.save(Assignment
                .builder()
                .passengerId(passenger.getId())
                .address("서울시 강남구 강남역 3번출구")
                .driverId(null)
                .status(Assignment.Status.WAITING)
                .requestDt(LocalDateTime.now())
                .build());

        completedAssignment = assignmentRepository.save(Assignment
                .builder()
                .passengerId(passenger2.getId())
                .driverId(driver.getId())
                .address("서울시 강남구 강남역 3번출구")
                .status(Assignment.Status.COMPLETE)
                .requestDt(LocalDateTime.now().minusMinutes(10))
                .completeDt(LocalDateTime.now())
                .build());
    }

    @Test
    public void 배차목록_가져오기() {
        // when
        List<AssignmentDto.Res> resList = assignmentService.getAll();
        // then
        assertThat(resList.size()).isEqualTo(2);
        assertThat(resList.get(0).getPassengerId()).isEqualTo(waitingAssignment.getPassengerId());
        assertThat(resList.get(0).getDriverId()).isEqualTo(waitingAssignment.getDriverId());
        assertThat(resList.get(0).getStatus()).isEqualTo(waitingAssignment.getStatus());
        assertThat(resList.get(0).getRequestDt()).isEqualTo(waitingAssignment.getRequestDt());
        assertThat(resList.get(0).getCompleteDt()).isEqualTo(waitingAssignment.getCompleteDt());

        assertThat(resList.get(1).getPassengerId()).isEqualTo(completedAssignment.getPassengerId());
        assertThat(resList.get(1).getDriverId()).isEqualTo(completedAssignment.getDriverId());
        assertThat(resList.get(1).getStatus()).isEqualTo(completedAssignment.getStatus());
        assertThat(resList.get(1).getRequestDt()).isEqualTo(completedAssignment.getRequestDt());
        assertThat(resList.get(1).getCompleteDt()).isEqualTo(completedAssignment.getCompleteDt());
    }

    @Test(expected = IllegalArgumentException.class)
    public void 배차요청_승객이아닐경우() {
        //given
        AssignmentDto.Req req = AssignmentDto.Req
                .builder()
                .address("서울시 강남구 강남역 3번출구")
                .build();

        //when
        assignmentService.requestAssignment(driver, req);
    }

    @Test(expected = IllegalArgumentException.class)
    public void 배차요청_승객_배차중일경우() {
        // given
        AssignmentDto.Req req = AssignmentDto.Req
                .builder()
                .address("서울시 강남구 강남역 3번출구")
                .build();

        // when
        assignmentService.requestAssignment(passenger, req);
    }

    @Test
    public void 배차요청_승객_배차() {
        // given
        AssignmentDto.Req req = AssignmentDto.Req
                .builder()
                .address("서울시 강남구 강남역 3번출구")
                .build();

        // when
        AssignmentDto.Res res = assignmentService.requestAssignment(passenger2, req);

        // then
        assertThat(res.getId()).isNotNull();
        assertThat(res.getPassengerId()).isEqualTo(passenger2.getId());
        assertThat(res.getDriverId()).isNull();
        assertThat(res.getAddress()).isEqualTo(req.getAddress());
        assertThat(res.getStatus()).isEqualTo(Assignment.Status.WAITING);
        assertThat(res.getRequestDt()).isNotNull();
        assertThat(res.getCompleteDt()).isNull();
    }

    @Test(expected = IllegalArgumentException.class)
    public void 배차확인_기사가아닐경우() {
        // when
        assignmentService.catchAssignment(passenger, waitingAssignment.getId());
    }

    @Test(expected = IllegalArgumentException.class)
    public void 배차확인_배차아이디가잘못된경우() {
        // given
        Long assignmentId = 9999999L;
        // when
        assignmentService.catchAssignment(driver, assignmentId);
    }

    @Test(expected = IllegalArgumentException.class)
    public void 배차확인_이미배차가완료된경우() {
        // when
        assignmentService.catchAssignment(driver, completedAssignment.getId());
    }

    @Test
    public void 배차확인() {
        // when
        AssignmentDto.Res res = assignmentService.catchAssignment(driver, waitingAssignment.getId());
        // then
        assertThat(res.getId()).isNotNull();
        assertThat(res.getPassengerId()).isEqualTo(waitingAssignment.getPassengerId());
        assertThat(res.getDriverId()).isEqualTo(driver.getId());
        assertThat(res.getAddress()).isEqualTo(waitingAssignment.getAddress());
        assertThat(res.getStatus()).isEqualTo(Assignment.Status.COMPLETE);
        assertThat(res.getRequestDt()).isNotNull();
        assertThat(res.getCompleteDt()).isNotNull();
    }

}