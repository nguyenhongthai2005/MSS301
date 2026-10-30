package fu.de190299.room.service.impl;

import fu.de190299.room.dto.ApiResponseDTO;
import fu.de190299.room.dto.PageDTO;
import fu.de190299.room.dto.RoomDTO;
import fu.de190299.room.entity.Room;
import fu.de190299.room.entity.RoomStatus;
import fu.de190299.room.entity.RoomType;
import fu.de190299.room.repository.RoomRepository;
import fu.de190299.room.service.RoomService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class RoomServiceImpl implements RoomService {

    private final RoomRepository roomRepository;

    public RoomServiceImpl(RoomRepository roomRepository) {
        this.roomRepository = roomRepository;
    }

    @Override
    @Transactional
    public ApiResponseDTO createRoom(RoomDTO dto) {
        // [KHAC BAI 1]: O bai 1 trung username tra ve status = 2.
        // O bai 2 (Room) theo de thi: trung roomNumber tra ve status = 3 (HTTP 226).
        if (roomRepository.existsByRoomNumber(dto.getRoomNumber())) {
            return ApiResponseDTO.error(3, "Room number is duplicated");
        }

        Room r = new Room();
        r.setRoomNumber(dto.getRoomNumber());
        r.setRoomType(RoomType.valueOf(dto.getRoomType()));
        r.setPricePerNight(dto.getPricePerNight());
        r.setCapacity(dto.getCapacity());
        r.setFloor(dto.getFloor());

        // [KHAC BAI 1]: Mặc định status phòng là AVAILABLE theo yeu cau de bai
        if (dto.getStatus() != null && !dto.getStatus().isBlank()) {
            r.setStatus(RoomStatus.valueOf(dto.getStatus()));
        } else {
            r.setStatus(RoomStatus.AVAILABLE);
        }

        Room saved = roomRepository.save(r);
        return ApiResponseDTO.of(1, "Room created successfully", toDTO(saved));
    }

    @Override
    @Transactional
    public ApiResponseDTO updateRoom(Long roomId, RoomDTO dto) {
        Room r = roomRepository.findById(roomId).orElse(null);
        if (r == null) {
            return ApiResponseDTO.error(4, "Room is not found");
        }

        // [KHAC BAI 1]: Check trung roomNumber tra ve status = 3
        if (dto.getRoomNumber() != null &&
                roomRepository.existsByRoomNumberAndRoomIdNot(dto.getRoomNumber(), roomId)) {
            return ApiResponseDTO.error(3, "Room number is duplicated");
        }

        // Partial update: Only provided fields are updated
        if (dto.getRoomNumber() != null && !dto.getRoomNumber().isBlank()) {
            r.setRoomNumber(dto.getRoomNumber());
        }
        if (dto.getRoomType() != null && !dto.getRoomType().isBlank()) {
            r.setRoomType(RoomType.valueOf(dto.getRoomType()));
        }
        if (dto.getPricePerNight() != null) {
            r.setPricePerNight(dto.getPricePerNight());
        }
        if (dto.getCapacity() != null) {
            r.setCapacity(dto.getCapacity());
        }
        if (dto.getFloor() != null) {
            r.setFloor(dto.getFloor());
        }
        if (dto.getStatus() != null && !dto.getStatus().isBlank()) {
            r.setStatus(RoomStatus.valueOf(dto.getStatus()));
        }

        Room saved = roomRepository.save(r);
        return ApiResponseDTO.of(1, "Room updated successfully", toDTO(saved));
    }

    @Override
    @Transactional(readOnly = true)
    public ApiResponseDTO getRoomById(Long roomId) {
        Room r = roomRepository.findById(roomId).orElse(null);
        if (r == null) {
            return ApiResponseDTO.error(4, "Room is not found");
        }
        return ApiResponseDTO.ok("Room retrieved successfully", toDTO(r));
    }

    @Override
    @Transactional
    public ApiResponseDTO setMaintenance(Long roomId) {
        // [KHAC BAI 1 CUC KY QUAN TRONG]:
        // Bai 1: DELETE la xoa cung (deleteById) khoi Database.
        // Bai 2 (Room): DELETE /api/rooms/{id} la SOFT-UPDATE -> set status thanh MAINTENANCE!
        Room r = roomRepository.findById(roomId).orElse(null);
        if (r == null) {
            return ApiResponseDTO.error(4, "Room is not found");
        }
        r.setStatus(RoomStatus.MAINTENANCE);
        roomRepository.save(r);
        return ApiResponseDTO.ok("Room set to MAINTENANCE successfully", null);
    }

    @Override
    @Transactional(readOnly = true)
    public ApiResponseDTO getRooms(int page, int size, String roomType, String status) {
        if (page < 0) page = 0;
        if (size < 1) size = 10;
        if (size > 100) size = 100;

        RoomType typeEnum = null;
        if (roomType != null && !roomType.isBlank()) {
            try {
                typeEnum = RoomType.valueOf(roomType.toUpperCase());
            } catch (IllegalArgumentException ignored) {}
        }

        RoomStatus statusEnum = null;
        if (status != null && !status.isBlank()) {
            try {
                statusEnum = RoomStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException ignored) {}
        }

        Pageable pageable = PageRequest.of(page, size);
        Page<Room> resultPage = roomRepository.search(typeEnum, statusEnum, pageable);

        List<RoomDTO> content = resultPage.getContent().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());

        PageDTO pageDTO = new PageDTO(
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

    private RoomDTO toDTO(Room r) {
        RoomDTO d = new RoomDTO();
        d.setRoomId(r.getRoomId());
        d.setRoomNumber(r.getRoomNumber());
        d.setRoomType(r.getRoomType().name());
        d.setPricePerNight(r.getPricePerNight());
        d.setCapacity(r.getCapacity());
        d.setFloor(r.getFloor());
        d.setStatus(r.getStatus().name());
        return d;
    }
}
