package fu.de190299.reservation.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import fu.de190299.reservation.client.RoomClient;
import fu.de190299.reservation.common.ReservationConstants;
import fu.de190299.reservation.dto.*;
import fu.de190299.reservation.entity.Reservation;
import fu.de190299.reservation.entity.ReservationStatus;
import fu.de190299.reservation.repository.ReservationRepository;
import fu.de190299.reservation.service.ReservationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ReservationServiceImpl implements ReservationService {

    private final ReservationRepository reservationRepository;
    private final RoomClient roomClient;
    private final ObjectMapper objectMapper;

    public ReservationServiceImpl(ReservationRepository reservationRepository,
                                  RoomClient roomClient,
                                  ObjectMapper objectMapper) {
        this.reservationRepository = reservationRepository;
        this.roomClient = roomClient;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public ApiResponseDTO createReservation(ReservationDTO dto) {
        // [RANG BUOC 1]: checkOutDate phai sau checkInDate
        if (!dto.getCheckOutDate().after(dto.getCheckInDate())) {
            return ApiResponseDTO.error(2, "checkOutDate must be after checkInDate");
        }

        // [FEIGN CALL]: Goi RoomService de lay thong tin phong
        RoomDTO room = fetchRoom(dto.getRoomId());
        if (room == null) {
            // [KHAC BAI 1]: Tra ve status = 4 (HTTP 404) khi phong khong ton tai
            return ApiResponseDTO.error(4, "Room ID is not found");
        }

        // [RANG BUOC 2]: Phong phai o trang thai AVAILABLE
        if (!ReservationConstants.ROOM_AVAILABLE.equalsIgnoreCase(room.getStatus())) {
            // Tra ve status = 5 (HTTP 400) khi phong khong AVAILABLE
            return ApiResponseDTO.error(5, "Room is not AVAILABLE for reservation");
        }

        // [RANG BUOC 3 - KHAC BAI 1]:
        // O bai 1: kiem tra numberOfPosts <= user.maxDailyPosts.
        // O bai 2: kiem tra numberOfGuests <= room.capacity (suc chua phong).
        if (dto.getNumberOfGuests() > room.getCapacity()) {
            return ApiResponseDTO.error(2, "numberOfGuests must not exceed the room capacity");
        }

        Reservation r = new Reservation();
        r.setGuestName(dto.getGuestName());
        r.setGuestEmail(dto.getGuestEmail());
        r.setGuestPhone(dto.getGuestPhone());
        r.setCheckInDate(dto.getCheckInDate());
        r.setCheckOutDate(dto.getCheckOutDate());
        r.setNumberOfGuests(dto.getNumberOfGuests());
        r.setRoomId(dto.getRoomId());

        // [TINH TIEN - KHAC BAI 1]:
        // Bai 1: totalCost = ratePerPost * so ngay
        // Bai 2: totalAmount = pricePerNight * so dem (nights = checkOutDate - checkInDate)
        r.setTotalAmount(computeTotal(room.getPricePerNight(), dto.getCheckInDate(), dto.getCheckOutDate()));

        // [TRANG THAI MAC DINH - KHAC BAI 1]:
        // Bai 1: Mac dinh la SCHEDULED
        // Bai 2: Mac dinh la CONFIRMED theo de thi
        r.setStatus(ReservationStatus.CONFIRMED);

        Reservation saved = reservationRepository.save(r);
        return ApiResponseDTO.of(1, "Reservation created successfully", toDTO(saved));
    }

    @Override
    @Transactional
    public ApiResponseDTO updateReservation(Long reservationId, ReservationDTO dto) {
        Reservation r = reservationRepository.findById(reservationId).orElse(null);
        if (r == null) {
            return ApiResponseDTO.error(4, "Reservation ID is not found");
        }

        Date effectiveIn = dto.getCheckInDate() != null ? dto.getCheckInDate() : r.getCheckInDate();
        Date effectiveOut = dto.getCheckOutDate() != null ? dto.getCheckOutDate() : r.getCheckOutDate();

        if (!effectiveOut.after(effectiveIn)) {
            return ApiResponseDTO.error(2, "checkOutDate must be after checkInDate");
        }

        if (dto.getGuestName() != null) r.setGuestName(dto.getGuestName());
        if (dto.getGuestEmail() != null) r.setGuestEmail(dto.getGuestEmail());
        if (dto.getGuestPhone() != null) r.setGuestPhone(dto.getGuestPhone());
        r.setCheckInDate(effectiveIn);
        r.setCheckOutDate(effectiveOut);
        if (dto.getNumberOfGuests() != null) r.setNumberOfGuests(dto.getNumberOfGuests());
        if (dto.getStatus() != null) r.setStatus(ReservationStatus.valueOf(dto.getStatus()));

        // Tinh lai tien theo gia phong hien tai
        RoomDTO room = fetchRoom(r.getRoomId());
        if (room != null) {
            r.setTotalAmount(computeTotal(room.getPricePerNight(), r.getCheckInDate(), r.getCheckOutDate()));
        }

        Reservation saved = reservationRepository.save(r);
        return ApiResponseDTO.of(1, "Reservation updated successfully", toDTO(saved));
    }

    @Override
    @Transactional(readOnly = true)
    public ApiResponseDTO getReservationById(Long reservationId) {
        Reservation r = reservationRepository.findById(reservationId).orElse(null);
        if (r == null) {
            return ApiResponseDTO.error(4, "Reservation is not found");
        }
        return ApiResponseDTO.ok("Reservation retrieved successfully", toDetailDTO(r));
    }

    @Override
    @Transactional
    public ApiResponseDTO cancelReservation(Long reservationId) {
        // [KHAC BAI 1 CUC KY QUAN TRONG]:
        // Bai 1: DELETE post la xoa cung (deleteById) khoi Database.
        // Bai 2 (Reservation): DELETE /api/reservations/{id} la SOFT-UPDATE:
        // Set trang thai thanh CANCELLED!
        Reservation r = reservationRepository.findById(reservationId).orElse(null);
        if (r == null) {
            return ApiResponseDTO.error(4, "Reservation is not found");
        }
        r.setStatus(ReservationStatus.CANCELLED);
        reservationRepository.save(r);
        return ApiResponseDTO.ok("Reservation cancelled successfully", null);
    }

    @Override
    @Transactional(readOnly = true)
    public ApiResponseDTO getReservations(int page, int size, String guestName, String status) {
        if (page < 0) page = 0;
        if (size < 1) size = 10;
        if (size > 100) size = 100;

        ReservationStatus statusEnum = null;
        if (status != null && !status.isBlank()) {
            try {
                statusEnum = ReservationStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException ignored) {}
        }

        Pageable pageable = PageRequest.of(page, size);
        Page<Reservation> resultPage = reservationRepository.search(guestName, statusEnum, pageable);

        // [KHAC BAI 1]: De thi yeu cau GET danh sach tra ve ReservationDetailDTO (co kem RoomDTO)
        List<ReservationDetailDTO> content = resultPage.getContent().stream()
                .map(this::toDetailDTO)
                .collect(Collectors.toList());

        PageDTO<ReservationDetailDTO> pageDTO = new PageDTO<>(
                resultPage.getSize(),
                resultPage.getNumber(),
                resultPage.getTotalPages(),
                resultPage.getTotalElements(),
                resultPage.isFirst(),
                resultPage.isLast(),
                content
        );

        return ApiResponseDTO.ok("Successful", pageDTO);
    }

    // -- helpers --

    private RoomDTO fetchRoom(Long roomId) {
        try {
            ApiResponseDTO res = roomClient.getRoomById(roomId);
            if (res == null || res.getStatus() != 1 || res.getData() == null) {
                return null;
            }
            return objectMapper.convertValue(res.getData(), RoomDTO.class);
        } catch (FeignException.NotFound ex) {
            return null; // Room khong ton tai (status = 4)
        } catch (Exception ex) {
            return null;
        }
    }

    // [CONG THUC TINH TIEN]: pricePerNight * so dem
    private BigDecimal computeTotal(BigDecimal pricePerNight, Date checkIn, Date checkOut) {
        long nights = ChronoUnit.DAYS.between(toLocalDate(checkIn), toLocalDate(checkOut));
        if (nights < 1) nights = 1;
        return pricePerNight.multiply(BigDecimal.valueOf(nights));
    }

    private LocalDate toLocalDate(Date date) {
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    private ReservationDTO toDTO(Reservation r) {
        ReservationDTO d = new ReservationDTO();
        d.setReservationId(r.getReservationId());
        d.setGuestName(r.getGuestName());
        d.setGuestEmail(r.getGuestEmail());
        d.setGuestPhone(r.getGuestPhone());
        d.setCheckInDate(r.getCheckInDate());
        d.setCheckOutDate(r.getCheckOutDate());
        d.setNumberOfGuests(r.getNumberOfGuests());
        d.setTotalAmount(r.getTotalAmount());
        d.setStatus(r.getStatus().name());
        d.setRoomId(r.getRoomId());
        return d;
    }

    private ReservationDetailDTO toDetailDTO(Reservation r) {
        ReservationDetailDTO d = new ReservationDetailDTO();
        d.setReservationId(r.getReservationId());
        d.setGuestName(r.getGuestName());
        d.setGuestEmail(r.getGuestEmail());
        d.setGuestPhone(r.getGuestPhone());
        d.setCheckInDate(r.getCheckInDate());
        d.setCheckOutDate(r.getCheckOutDate());
        d.setNumberOfGuests(r.getNumberOfGuests());
        d.setTotalAmount(r.getTotalAmount());
        d.setStatus(r.getStatus().name());
        d.setRoom(fetchRoom(r.getRoomId()));
        return d;
    }
}
