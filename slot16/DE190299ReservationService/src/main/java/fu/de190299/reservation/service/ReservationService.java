package fu.de190299.reservation.service;

import fu.de190299.reservation.dto.ApiResponseDTO;
import fu.de190299.reservation.dto.ReservationDTO;

public interface ReservationService {

    ApiResponseDTO createReservation(ReservationDTO dto);

    ApiResponseDTO updateReservation(Long reservationId, ReservationDTO dto);

    ApiResponseDTO getReservationById(Long reservationId);

    ApiResponseDTO cancelReservation(Long reservationId);

    ApiResponseDTO getReservations(int page, int size, String guestName, String status);
}
