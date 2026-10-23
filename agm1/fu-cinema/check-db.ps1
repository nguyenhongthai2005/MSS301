# ==============================================================================
# Script kiem tra du lieu thuc te trong 3 co so du lieu cua FUCinema
# ==============================================================================

Write-Host "`n========================================================" -ForegroundColor Cyan
Write-Host "   1. SQL SERVER (customer-service - Port 14333)       " -ForegroundColor Cyan
Write-Host "========================================================" -ForegroundColor Cyan
docker exec cinema-sqlserver /opt/mssql-tools18/bin/sqlcmd -S localhost -U sa -P "Fucinema@2026" -C -d cinema_customer -W -Q "SET NOCOUNT ON; SELECT customer_id AS ID, customer_name AS Name, email AS Email, telephone AS Phone, customer_status AS Status FROM customer;"

Write-Host "`n========================================================" -ForegroundColor Yellow
Write-Host "   2. MONGODB (movie-service - Port 27017)             " -ForegroundColor Yellow
Write-Host "========================================================" -ForegroundColor Yellow
Write-Host "--- The loai (genres) ---" -ForegroundColor DarkYellow
docker exec cinema-mongo mongosh -u root -p password --authenticationDatabase admin cinema_movie --quiet --eval "db.genres.find().forEach(g => print('  [' + g._id + '] ' + g.genreName + ' - ' + (g.description || '')))"

Write-Host "`n--- Danh sach Phim (movies) ---" -ForegroundColor DarkYellow
docker exec cinema-mongo mongosh -u root -p password --authenticationDatabase admin cinema_movie --quiet --eval "db.movies.find().forEach(m => print('  [' + m._id + '] ' + m.title + ' (' + m.durationMinutes + ' mins) - Status: ' + m.movieStatus))"

Write-Host "`n--- Danh sach Phong chieu (cinema_rooms) ---" -ForegroundColor DarkYellow
docker exec cinema-mongo mongosh -u root -p password --authenticationDatabase admin cinema_movie --quiet --eval "db.cinema_rooms.find().forEach(r => print('  [' + r._id + '] ' + r.roomName + ' (' + r.roomType + ') - ' + (r.seatRows * r.seatsPerRow) + ' seats (' + r.seatRows + 'x' + r.seatsPerRow + ') - ' + r.roomStatus))"

Write-Host "`n--- Danh sach Suat chieu (showtimes) ---" -ForegroundColor DarkYellow
docker exec cinema-mongo mongosh -u root -p password --authenticationDatabase admin cinema_movie --quiet --eval "db.showtimes.find().forEach(s => print('  [' + s._id + '] MovieId: ' + s.movieId + ' | RoomId: ' + s.roomId + ' | Start: ' + s.startTime.toISOString() + ' | Price: ' + s.ticketPrice + ' VND | Status: ' + s.showtimeStatus))"

Write-Host "`n========================================================" -ForegroundColor Green
Write-Host "   3. MYSQL (booking-service - Port 3307)              " -ForegroundColor Green
Write-Host "========================================================" -ForegroundColor Green
Write-Host "--- Danh sach Don dat ve (booking) ---" -ForegroundColor DarkGreen
docker exec -e MYSQL_PWD=mysql cinema-mysql mysql -u root cinema_booking -e "SELECT booking_id AS ID, customer_id AS CustID, total_price AS Total, booking_status AS Status, booking_date AS BookingDate FROM booking;"
$bCount = docker exec -e MYSQL_PWD=mysql cinema-mysql mysql -u root cinema_booking -N -s -e "SELECT COUNT(*) FROM booking;"
if ($bCount -eq 0) {
    Write-Host "  (Bang booking hien dang TRONG [0 ve] - Hay chay request Dat ve tren Postman de them du lieu!)" -ForegroundColor Gray
}

Write-Host "`n--- Chi tiet ghe dat (booking_detail) ---" -ForegroundColor DarkGreen
docker exec -e MYSQL_PWD=mysql cinema-mysql mysql -u root cinema_booking -e "SELECT booking_detail_id AS DetailID, booking_id AS BookingID, seat_code AS Seat, movie_title AS Movie, room_name AS Room, price AS Price FROM booking_detail;"
$dCount = docker exec -e MYSQL_PWD=mysql cinema-mysql mysql -u root cinema_booking -N -s -e "SELECT COUNT(*) FROM booking_detail;"
if ($dCount -eq 0) {
    Write-Host "  (Bang booking_detail hien dang TRONG [0 ghe])" -ForegroundColor Gray
}

Write-Host "`nKiem tra hoan tat!`n" -ForegroundColor Cyan
