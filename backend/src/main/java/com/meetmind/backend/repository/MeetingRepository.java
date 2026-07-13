package com.meetmind.backend.repository;

import com.meetmind.backend.entity.Meeting;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MeetingRepository extends JpaRepository<Meeting, Long>
{
}