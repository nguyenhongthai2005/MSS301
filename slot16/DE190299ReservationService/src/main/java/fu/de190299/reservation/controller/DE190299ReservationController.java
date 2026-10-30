package fu.de190299.reservation.controller;

import fu.de190299.reservation.dto.ApiResponseDTO;
import fu.de190299.reservation.dto.ReservationDTO;
import fu.de190299.reservation.service.ReservationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reservations")
public class DE190299ReservationController {

    private final ReservationService reservationService;

    public DE190299ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @PostMapping
    public ResponseEntity<ApiResponseDTO> createReservation(@Valid @RequestBody ReservationDTO dto) {
        ApiResponseDTO res = reservationService.createReservation(dto);
        return toResponseEntity(res, HttpStatus.CREATED);
    }

    @PutMapping("/{reservationId}")
    public ResponseEntity<ApiResponseDTO> updateReservation(@PathVariable("reservationId") Long reservationId,
                                                            @RequestBody ReservationDTO dto) {
        ApiResponseDTO res = reservationService.updateReservation(reservationId, dto);
        return toResponseEntity(res, HttpStatus.OK);
    }

    @GetMapping("/{reservationId}")
    public ResponseEntity<ApiResponseDTO> getReservationDetail(@PathVariable("reservationId") Long reservationId) {
        ApiResponseDTO res = reservationService.getReservationById(reservationId);
        return toResponseEntity(res, HttpStatus.OK);
    }

    // [KHAC BAI 1]: DELETE la Soft-Cancel sang status CANCELLED (tra ve 200 OK)
    @DeleteMapping("/{reservationId}")
    public ResponseEntity<ApiResponseDTO> cancelReservation(@PathVariable("reservationId") Long reservationId) {
        ApiResponseDTO res = reservationService.cancelReservation(reservationId);
        return toResponseEntity(res, HttpStatus.OK);
    }

    @GetMapping
    public ResponseEntity<ApiResponseDTO> getReservations(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            @RequestParam(name = "guestName", required = false) String guestName,
            @RequestParam(name = "status", required = false) String status) {
        ApiResponseDTO res = reservationService.getReservations(page, size, guestName, status);
        return toResponseEntity(res, HttpStatus.OK);
    }

    // [MAPPING STATUS THEO DE THI]:
    // status = 1 -> HttpStatus.OK / CREATED
    // status = 2 -> HttpStatus.NOT_ACCEPTABLE (406) - Validation failed / capacity exceeded
    // status = 4 -> HttpStatus.NOT_FOUND (404) - Room or Reservation not found
    // status = 5 -> HttpStatus.BAD_REQUEST (400) - Room is not AVAILABLE
    // status = 0 -> HttpStatus.INTERNAL_SERVER_ERROR (500)
    private ResponseEntity<ApiResponseDTO> toResponseEntity(ApiResponseDTO res, HttpStatus successStatus) {
        if (res.getStatus() == 1) {
            return ResponseEntity.status(successStatus).body(res);
        }
        if (res.getStatus() == 2) {
            return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).body(res); // 406
        }
        if (res.getStatus() == 4) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(res); // 404
        }
        if (res.getStatus() == 5) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(res); // 400
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(res); // 500
    }
}
