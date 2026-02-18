-- =============================================
-- FotoLab Database Schema
-- =============================================
-- Default Admin credentials:
--   Username: admin
--   Password: admin
-- =============================================

CREATE DATABASE IF NOT EXISTS `fotolab`;
USE `fotolab`;

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

-- --------------------------------------------------------
-- Table structure for table `users`
-- --------------------------------------------------------

CREATE TABLE `users` (
  `id` int(11) NOT NULL,
  `username` varchar(255) NOT NULL,
  `password` varchar(255) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------
-- Table structure for table `roles`
-- --------------------------------------------------------

CREATE TABLE `roles` (
  `id` int(11) NOT NULL,
  `role` enum('Admin','Member') NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------
-- Table structure for table `userrole`
-- --------------------------------------------------------

CREATE TABLE `userrole` (
  `userid` int(11) NOT NULL,
  `roleid` int(11) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------
-- Table structure for table `albums`
-- --------------------------------------------------------

CREATE TABLE `albums` (
  `id` int(11) NOT NULL,
  `userid` int(11) NOT NULL,
  `title` varchar(255) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------
-- Table structure for table `albumtags`
-- --------------------------------------------------------

CREATE TABLE `albumtags` (
  `id` int(11) NOT NULL,
  `albumid` int(11) NOT NULL,
  `tag` varchar(255) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------
-- Table structure for table `images`
-- --------------------------------------------------------

CREATE TABLE `images` (
  `id` int(11) NOT NULL,
  `userid` int(11) NOT NULL,
  `title` varchar(255) NOT NULL,
  `date` date NOT NULL,
  `path` varchar(255) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------
-- Table structure for table `imagetags`
-- --------------------------------------------------------

CREATE TABLE `imagetags` (
  `id` int(11) NOT NULL,
  `imageid` int(11) NOT NULL,
  `tag` varchar(255) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------
-- Table structure for table `albumimages`
-- --------------------------------------------------------

CREATE TABLE `albumimages` (
  `imageid` int(11) NOT NULL,
  `albumid` int(11) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------
-- Indexes
-- --------------------------------------------------------

ALTER TABLE `users`
  ADD PRIMARY KEY (`id`,`username`) USING BTREE;

ALTER TABLE `roles`
  ADD PRIMARY KEY (`id`);

ALTER TABLE `userrole`
  ADD PRIMARY KEY (`userid`,`roleid`),
  ADD KEY `role_fk_roleid` (`roleid`,`userid`) USING BTREE;

ALTER TABLE `albums`
  ADD PRIMARY KEY (`id`),
  ADD KEY `albums_fk_userid` (`userid`) USING BTREE;

ALTER TABLE `albumtags`
  ADD PRIMARY KEY (`id`),
  ADD KEY `albumtags_fk_albumid` (`albumid`);

ALTER TABLE `images`
  ADD PRIMARY KEY (`id`),
  ADD KEY `img_fk_userid` (`userid`) USING BTREE;

ALTER TABLE `imagetags`
  ADD PRIMARY KEY (`id`),
  ADD KEY `imgtags_fk_images` (`imageid`);

ALTER TABLE `albumimages`
  ADD PRIMARY KEY (`imageid`,`albumid`),
  ADD KEY `fotoalbum_fk_albumid` (`albumid`,`imageid`) USING BTREE;

-- --------------------------------------------------------
-- AUTO_INCREMENT
-- --------------------------------------------------------

ALTER TABLE `users`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=2;

ALTER TABLE `roles`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=3;

ALTER TABLE `albums`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

ALTER TABLE `albumtags`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

ALTER TABLE `images`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

ALTER TABLE `imagetags`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

-- --------------------------------------------------------
-- Foreign Key Constraints
-- --------------------------------------------------------

ALTER TABLE `userrole`
  ADD CONSTRAINT `role_fk_roleid` FOREIGN KEY (`roleid`) REFERENCES `roles` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  ADD CONSTRAINT `role_fk_userid` FOREIGN KEY (`userid`) REFERENCES `users` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;

ALTER TABLE `albums`
  ADD CONSTRAINT `fk_userids` FOREIGN KEY (`userid`) REFERENCES `users` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;

ALTER TABLE `albumtags`
  ADD CONSTRAINT `albumtags_fk_albumid` FOREIGN KEY (`albumid`) REFERENCES `albums` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;

ALTER TABLE `images`
  ADD CONSTRAINT `img_fk_userid` FOREIGN KEY (`userid`) REFERENCES `users` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;

ALTER TABLE `imagetags`
  ADD CONSTRAINT `imgtags_fk_images` FOREIGN KEY (`imageid`) REFERENCES `images` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;

ALTER TABLE `albumimages`
  ADD CONSTRAINT `fotoalbum_fk_albumid` FOREIGN KEY (`albumid`) REFERENCES `albums` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  ADD CONSTRAINT `fotoalbum_fk_imgid` FOREIGN KEY (`imageid`) REFERENCES `images` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;

-- --------------------------------------------------------
-- Seed Data
-- --------------------------------------------------------

-- Roles
INSERT INTO `roles` (`id`, `role`) VALUES
(1, 'Admin'),
(2, 'Member');

-- Admin user (username: admin, password: admin)
-- Password is bcrypt hashed with cost factor 10
INSERT INTO `users` (`id`, `username`, `password`) VALUES
(1, 'admin', '$2a$10$Rvogzqqd4Sly2QT2vxnXjOQhJyyHW/UHlqUSATEg6ojHXG1B2UOmG');

-- Assign Admin role to admin user
INSERT INTO `userrole` (`userid`, `roleid`) VALUES
(1, 1);

COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
