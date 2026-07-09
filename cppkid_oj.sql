-- MySQL dump 10.13  Distrib 8.0.32, for Win64 (x86_64)
--
-- Host: localhost    Database: cppkid_oj
-- ------------------------------------------------------
-- Server version	8.0.32

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Current Database: `cppkid_oj`
--

CREATE DATABASE /*!32312 IF NOT EXISTS*/ `cppkid_oj` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci */ /*!80016 DEFAULT ENCRYPTION='N' */;

USE `cppkid_oj`;

--
-- Table structure for table `class_group`
--

DROP TABLE IF EXISTS `class_group`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `class_group` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `teacher_id` bigint NOT NULL,
  `class_name` varchar(100) NOT NULL,
  `description` varchar(500) DEFAULT NULL,
  `status` tinyint NOT NULL DEFAULT '1',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_teacher_id` (`teacher_id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `class_group`
--

LOCK TABLES `class_group` WRITE;
/*!40000 ALTER TABLE `class_group` DISABLE KEYS */;
INSERT INTO `class_group` VALUES (1,1,'周五 18:30','0041',1,'2026-07-06 14:59:50','2026-07-06 14:59:50');
/*!40000 ALTER TABLE `class_group` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `class_member`
--

DROP TABLE IF EXISTS `class_member`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `class_member` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `class_id` bigint NOT NULL,
  `student_id` bigint NOT NULL,
  `joined_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_class_student` (`class_id`,`student_id`),
  KEY `idx_student_id` (`student_id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `class_member`
--

LOCK TABLES `class_member` WRITE;
/*!40000 ALTER TABLE `class_member` DISABLE KEYS */;
INSERT INTO `class_member` VALUES (1,1,2,'2026-07-06 15:06:53');
/*!40000 ALTER TABLE `class_member` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `contest`
--

DROP TABLE IF EXISTS `contest`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `contest` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `teacher_id` bigint NOT NULL,
  `class_id` bigint NOT NULL,
  `title` varchar(150) NOT NULL,
  `description` varchar(1000) DEFAULT NULL,
  `start_time` datetime NOT NULL,
  `end_time` datetime NOT NULL,
  `status` varchar(20) NOT NULL DEFAULT 'DRAFT',
  `show_rank` tinyint NOT NULL DEFAULT '1',
  `allow_submit_after_end` tinyint NOT NULL DEFAULT '0',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_teacher_id` (`teacher_id`),
  KEY `idx_class_status` (`class_id`,`status`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `contest`
--

LOCK TABLES `contest` WRITE;
/*!40000 ALTER TABLE `contest` DISABLE KEYS */;
INSERT INTO `contest` VALUES (1,1,1,'test','teset','2026-07-06 15:46:00','2026-07-06 15:47:02','PUBLISHED',1,0,'2026-07-06 15:45:12','2026-07-06 15:45:28'),(2,1,1,'test','test','2026-07-06 15:47:20','2026-07-06 15:49:23','PUBLISHED',1,0,'2026-07-06 15:47:30','2026-07-06 15:47:53'),(3,1,1,'test','test','2026-07-09 02:39:06','2026-07-16 02:39:09','PUBLISHED',1,1,'2026-07-06 16:23:28','2026-07-06 16:23:36');
/*!40000 ALTER TABLE `contest` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `contest_answer`
--

DROP TABLE IF EXISTS `contest_answer`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `contest_answer` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `contest_id` bigint NOT NULL,
  `problem_id` bigint NOT NULL,
  `student_id` bigint NOT NULL,
  `status` varchar(30) NOT NULL DEFAULT 'UNTRIED',
  `best_score` int NOT NULL DEFAULT '0',
  `submit_count` int NOT NULL DEFAULT '0',
  `first_open_at` datetime DEFAULT NULL,
  `first_submit_at` datetime DEFAULT NULL,
  `accepted_at` datetime DEFAULT NULL,
  `last_submit_at` datetime DEFAULT NULL,
  `hint_shown` tinyint NOT NULL DEFAULT '0',
  `hint_first_shown_at` datetime DEFAULT NULL,
  `final_submission_id` bigint DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_contest_problem_student` (`contest_id`,`problem_id`,`student_id`),
  KEY `idx_contest_student` (`contest_id`,`student_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `contest_answer`
--

LOCK TABLES `contest_answer` WRITE;
/*!40000 ALTER TABLE `contest_answer` DISABLE KEYS */;
INSERT INTO `contest_answer` VALUES (1,1,1,2,'TRIED',0,0,'2026-07-06 15:46:03',NULL,NULL,NULL,0,NULL,NULL,'2026-07-06 15:46:03','2026-07-06 15:46:03'),(2,2,1,2,'TRIED',0,0,'2026-07-06 15:48:02',NULL,NULL,NULL,1,'2026-07-06 15:49:39',NULL,'2026-07-06 15:48:02','2026-07-06 15:48:02'),(3,3,1,2,'ACCEPTED',100,23,'2026-07-06 16:23:50','2026-07-06 16:24:07','2026-07-06 18:41:35','2026-07-09 13:44:00',1,'2026-07-06 16:35:20',26,'2026-07-06 16:23:50','2026-07-06 16:23:50');
/*!40000 ALTER TABLE `contest_answer` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `contest_participant`
--

DROP TABLE IF EXISTS `contest_participant`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `contest_participant` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `contest_id` bigint NOT NULL,
  `student_id` bigint NOT NULL,
  `total_score` int NOT NULL DEFAULT '0',
  `accepted_count` int NOT NULL DEFAULT '0',
  `submit_count` int NOT NULL DEFAULT '0',
  `started_at` datetime DEFAULT NULL,
  `last_submit_at` datetime DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_contest_student` (`contest_id`,`student_id`),
  KEY `idx_student_id` (`student_id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `contest_participant`
--

LOCK TABLES `contest_participant` WRITE;
/*!40000 ALTER TABLE `contest_participant` DISABLE KEYS */;
INSERT INTO `contest_participant` VALUES (1,1,2,0,0,0,NULL,NULL,'2026-07-06 15:45:28','2026-07-06 15:45:28'),(2,2,2,0,0,0,NULL,NULL,'2026-07-06 15:47:53','2026-07-06 15:47:53'),(3,3,2,100,1,23,'2026-07-06 16:24:07','2026-07-09 13:44:00','2026-07-06 16:23:37','2026-07-09 13:43:59');
/*!40000 ALTER TABLE `contest_participant` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `contest_problem`
--

DROP TABLE IF EXISTS `contest_problem`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `contest_problem` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `contest_id` bigint NOT NULL,
  `problem_id` bigint NOT NULL,
  `display_order` int NOT NULL DEFAULT '0',
  `score` int NOT NULL DEFAULT '100',
  `hint_unlock_minutes` int NOT NULL DEFAULT '10',
  `show_hint_after_ac` tinyint NOT NULL DEFAULT '1',
  `show_hint_after_contest` tinyint NOT NULL DEFAULT '1',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_contest_problem` (`contest_id`,`problem_id`),
  KEY `idx_problem_id` (`problem_id`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `contest_problem`
--

LOCK TABLES `contest_problem` WRITE;
/*!40000 ALTER TABLE `contest_problem` DISABLE KEYS */;
INSERT INTO `contest_problem` VALUES (1,1,1,1,100,10,1,1,'2026-07-06 15:45:24'),(2,2,1,1,100,10,1,1,'2026-07-06 15:47:47'),(4,3,1,1,100,10,1,1,'2026-07-06 18:54:27');
/*!40000 ALTER TABLE `contest_problem` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `problem`
--

DROP TABLE IF EXISTS `problem`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `problem` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `title` varchar(150) NOT NULL,
  `description` mediumtext NOT NULL,
  `input_format` mediumtext,
  `output_format` mediumtext,
  `sample_input` mediumtext,
  `sample_output` mediumtext,
  `difficulty` varchar(20) NOT NULL DEFAULT 'easy',
  `time_limit_ms` int NOT NULL DEFAULT '2000',
  `memory_limit_mb` int NOT NULL DEFAULT '128',
  `compare_mode` varchar(50) NOT NULL DEFAULT 'ignore_trailing_space',
  `created_by` bigint NOT NULL,
  `visibility` varchar(20) NOT NULL DEFAULT 'private',
  `status` tinyint NOT NULL DEFAULT '1',
  `accepted_count` int NOT NULL DEFAULT '0',
  `submit_count` int NOT NULL DEFAULT '0',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_created_by` (`created_by`),
  KEY `idx_visibility` (`visibility`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `problem`
--

LOCK TABLES `problem` WRITE;
/*!40000 ALTER TABLE `problem` DISABLE KEYS */;
INSERT INTO `problem` VALUES (1,'A+B','这是一道入门题，计算 a+b 的和。','一行两个整数 a,b 。','一个整数，表示 a+b 的和','1 3','4','easy',2000,128,'ignore_trailing_space',1,'private',1,9,23,'2026-07-06 15:44:20','2026-07-09 13:43:59');
/*!40000 ALTER TABLE `problem` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `problem_hint`
--

DROP TABLE IF EXISTS `problem_hint`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `problem_hint` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `problem_id` bigint NOT NULL,
  `teacher_id` bigint NOT NULL,
  `hint_title` varchar(100) DEFAULT NULL,
  `hint_content` mediumtext NOT NULL,
  `hint_level` int NOT NULL DEFAULT '1',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_problem_level` (`problem_id`,`hint_level`),
  KEY `idx_teacher_id` (`teacher_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `problem_hint`
--

LOCK TABLES `problem_hint` WRITE;
/*!40000 ALTER TABLE `problem_hint` DISABLE KEYS */;
/*!40000 ALTER TABLE `problem_hint` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `submission`
--

DROP TABLE IF EXISTS `submission`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `submission` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `problem_id` bigint NOT NULL,
  `contest_id` bigint DEFAULT NULL,
  `language` varchar(30) NOT NULL,
  `code` mediumtext NOT NULL,
  `status` varchar(40) NOT NULL DEFAULT 'PENDING',
  `score` int NOT NULL DEFAULT '0',
  `time_used_ms` int NOT NULL DEFAULT '0',
  `memory_used_kb` int NOT NULL DEFAULT '0',
  `error_message` mediumtext,
  `judged_at` datetime DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_problem_id` (`problem_id`),
  KEY `idx_contest_user` (`contest_id`,`user_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB AUTO_INCREMENT=27 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `submission`
--

LOCK TABLES `submission` WRITE;
/*!40000 ALTER TABLE `submission` DISABLE KEYS */;
INSERT INTO `submission` VALUES (4,2,1,3,'cpp17','#include <iostream>\nusing namespace std;\n\nint main() {\n    int a,b;\n    cin >> a >> b;\n    cout << a + b;\n    return 0;\n}\n','COMPILE_ERROR',0,0,0,'docker: error during connect: this error may indicate that the docker daemon is not running: Post \"http://%2F%2F.%2Fpipe%2Fdocker_engine/v1.24/containers/create\": open //./pipe/docker_engine: The system cannot find the file specified.\nSee \'docker run --help\'.\n','2026-07-06 16:24:08','2026-07-06 16:24:07','2026-07-06 16:24:07'),(5,2,1,3,'cpp17','#include <iostream>\nusing namespace std;\n\nint main() {\n    int a,b;\n    cin >> a >> b;\n    cout << a + b;\n    return 0;\n}\n','COMPILE_ERROR',0,0,0,'docker: error during connect: this error may indicate that the docker daemon is not running: Post \"http://%2F%2F.%2Fpipe%2Fdocker_engine/v1.24/containers/create\": open //./pipe/docker_engine: The system cannot find the file specified.\nSee \'docker run --help\'.\n','2026-07-06 16:24:32','2026-07-06 16:24:31','2026-07-06 16:24:31'),(6,2,1,3,'cpp17','#include <iostream>\nusing namespace std;\n\nint main() {\n    int a,b;\n    cin >> a >> b;\n    cout << a + b;\n    \n    return 0;\n}\n','COMPILE_ERROR',0,0,0,'process timeout','2026-07-06 17:08:29','2026-07-06 17:08:08','2026-07-06 17:08:08'),(7,2,1,3,'cpp17','#include <iostream>\nusing namespace std;\n\nint main() {\n    int a,b;\n    cin >> a >> b;\n    cout << a + b;\n    return 0;\n}\n','COMPILE_ERROR',0,0,0,'process timeout','2026-07-06 18:24:00','2026-07-06 18:23:39','2026-07-06 18:23:39'),(8,2,1,3,'cpp17','#include <iostream>\nusing namespace std;\n\nint main() {\n    int a,b;\n    cin >> a >> b;\n    cout << a + b;\n    return 0;\n}\n','ACCEPTED',0,0,0,NULL,'2026-07-06 18:41:35','2026-07-06 18:41:33','2026-07-06 18:41:33'),(9,2,1,3,'cpp17','#include <iostream>\nusing namespace std;\n\nint main() {\n    int a,b;\n    cin >> a >> b;\n    cout << a + b + 1;\n    return 0;\n}\n','ACCEPTED',0,0,0,NULL,'2026-07-06 18:41:45','2026-07-06 18:41:44','2026-07-06 18:41:43'),(10,2,1,3,'cpp17','#include <iostream>\nusing namespace std;\n\nint main() {\n    int a,b;\n    cin >> a >> b;\n    cout << a + b + 1;\n    return 0;\n}\n','WRONG_ANSWER',0,415,0,NULL,'2026-07-06 18:54:51','2026-07-06 18:54:49','2026-07-06 18:54:49'),(11,2,1,3,'cpp17','#include <iostream>\nusing namespace std;\n\nint main() {\n    int a[10];\n    cin >> a[0] >> a[1];\n    a[-1] = 0;\n    cout << a[0] + a[1] + a[-1];\n    return 0;\n}\n','ACCEPTED',100,393,0,NULL,'2026-07-06 18:55:42','2026-07-06 18:55:40','2026-07-06 18:55:40'),(12,2,1,3,'cpp17','#include <iostream>\nusing namespace std;\n\nint main() {\n    int a[10];\n    for(int i = 1;i <= 10;++i){\n        a[i] += a[i + 1];\n    }\n\n    cin >> a[0] >> a[1];\n    a[-1] = 0;\n    cout << a[0] + a[1] + a[-1];\n    return 0;\n}\n','ACCEPTED',100,355,0,NULL,'2026-07-06 18:56:15','2026-07-06 18:56:14','2026-07-06 18:56:14'),(13,2,1,3,'cpp17','#include <iostream>\nusing namespace std;\n\nint main() {\n    int a,b;\n    cin >> a + b;\n    cout << a + b + c;\n    return 0;\n}\n','SYSTEM_ERROR',0,0,0,'判题环境 Docker 未启动或不可用，请联系老师或管理员','2026-07-09 02:39:36','2026-07-09 02:39:36','2026-07-09 02:39:35'),(14,2,1,3,'cpp17','#include <iostream>\nusing namespace std;\n\nint main() {\n    int a,b;\n    cin >> a + b;\n    cout << a + b;\n    return 0;\n}\n','SYSTEM_ERROR',0,0,0,'判题环境 Docker 未启动或不可用，请联系老师或管理员','2026-07-09 02:39:49','2026-07-09 02:39:48','2026-07-09 02:39:49'),(15,2,1,3,'cpp17','#include <iostream>\nusing namespace std;\n\nint main() {\n    int a,b;\n    cin >> a + b;\n    cout << a + b;\n    return 0;\n}\n','COMPILE_ERROR',0,0,0,'process timeout','2026-07-09 02:41:02','2026-07-09 02:40:41','2026-07-09 02:40:41'),(16,2,1,3,'cpp17','#include <iostream>\nusing namespace std;\n\nint main() {\n    int a,b;\n    cin >> a + b;\n    cout << a + b;\n    return 0;\n}\n','COMPILE_ERROR',0,0,0,'process timeout','2026-07-09 02:41:41','2026-07-09 02:41:20','2026-07-09 02:41:21'),(17,2,1,3,'cpp17','#include <iostream>\nusing namespace std;\n\nint main() {\n    int a,b;\n    cin >> a >> b;\n    cout << a + b;\n    return 0;\n}\n','ACCEPTED',100,439,0,NULL,'2026-07-09 02:42:35','2026-07-09 02:42:33','2026-07-09 02:42:33'),(18,2,1,3,'cpp17','#include <iostream>\nusing namespace std;\n\nint main() {\n    int a,b;\n    cin >> a >> b;\n    cout << a + b;\n    return 0;\n}\n','ACCEPTED',100,408,0,NULL,'2026-07-09 02:42:37','2026-07-09 02:42:33','2026-07-09 02:42:36'),(19,2,1,3,'cpp17','#include <iostream>\nusing namespace std;\n\nint main() {\n    int a,b;\n    cin >> a >> b;\n    cout << a + b + 1;\n    return 0;\n}\n','WRONG_ANSWER',0,437,0,NULL,'2026-07-09 02:42:53','2026-07-09 02:42:51','2026-07-09 02:42:51'),(20,2,1,3,'cpp17','#include <iostream>\nusing namespace std;\n\nint main() {\n    int a,b;\n    cin >> a >> b;\n    cout << a + b + c;\n    return 0;\n}\n','COMPILE_ERROR',0,0,0,'Main.cpp: In function \'int main()\':\nMain.cpp:7:21: error: \'c\' was not declared in this scope\n    7 |     cout << a + b + c;\n      |                     ^\n','2026-07-09 02:43:04','2026-07-09 02:43:02','2026-07-09 02:43:02'),(21,2,1,3,'cpp17','#include <iostream>\nusing namespace std;\n\nint main() {\n    int a,b;\n    cin >> a >> b;\n    cout << a+ b+1;\n    return 0;\n}\n','WRONG_ANSWER',0,591,0,NULL,'2026-07-09 02:58:50','2026-07-09 02:58:48','2026-07-09 02:58:48'),(22,2,1,3,'cpp17','#include <iostream>\nusing namespace std;\n\nint main() {\n    int a,b;\n    cin >> a >> b;\n    cout << a + b + c;\n    return 0;\n}\n','SYSTEM_ERROR',0,0,0,'判题环境 Docker 未启动或不可用，请联系老师或管理员','2026-07-09 13:32:30','2026-07-09 13:32:29','2026-07-09 13:32:29'),(23,2,1,3,'cpp17','#include <iostream>\nusing namespace std;\n\nint main() {\n    int a,b;\n    cin >> a >> b;\n    cout << a + b;\n    return 0;\n}\n','SYSTEM_ERROR',0,0,0,'判题环境 Docker 未启动或不可用，请联系老师或管理员','2026-07-09 13:32:40','2026-07-09 13:32:40','2026-07-09 13:32:40'),(24,2,1,3,'cpp17','#include <iostream>\nusing namespace std;\n\nint main() {\n    int a,b;\n    cin >> a >> b;\n    cout << a + b;\n    return 0;\n}\n','ACCEPTED',100,503,0,NULL,'2026-07-09 13:33:29','2026-07-09 13:33:26','2026-07-09 13:33:26'),(25,2,1,3,'cpp17','#include <iostream>\nusing namespace std;\n\nint main() {\n    int a,b;\n    cin >> a >> b;\n    long long res = 0;\n    for(int i = 1;i <= 1e7;++i){\n        res += i;\n    }\n    cout << a + b;\n    return 0;\n}\n','ACCEPTED',100,554,0,NULL,'2026-07-09 13:35:32','2026-07-09 13:35:29','2026-07-09 13:35:29'),(26,2,1,3,'cpp17','#include <iostream>\nusing namespace std;\n\nconst int N = 1E8;\n\nint arr[N];\n\nint main() {\n    arr[1000000] = -1;\n    int a,b;\n    cin >> a >> b;\n    long long res = 0;\n    for(int i = 1;i <= 1e7;++i){\n        res += i;\n    }\n    cout << a + b;\n    return 0;\n}\n','ACCEPTED',100,447,0,NULL,'2026-07-09 13:44:00','2026-07-09 13:43:58','2026-07-09 13:43:58');
/*!40000 ALTER TABLE `submission` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `submission_case`
--

DROP TABLE IF EXISTS `submission_case`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `submission_case` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `submission_id` bigint NOT NULL,
  `testcase_id` bigint NOT NULL,
  `status` varchar(40) NOT NULL,
  `time_used_ms` int NOT NULL DEFAULT '0',
  `memory_used_kb` int NOT NULL DEFAULT '0',
  `input_preview` text,
  `expected_output_preview` text,
  `actual_output_preview` text,
  `error_message` text,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_submission_id` (`submission_id`)
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `submission_case`
--

LOCK TABLES `submission_case` WRITE;
/*!40000 ALTER TABLE `submission_case` DISABLE KEYS */;
INSERT INTO `submission_case` VALUES (1,10,1,'WRONG_ANSWER',415,0,'5 6','11','12',NULL,'2026-07-06 18:54:51'),(2,11,1,'ACCEPTED',393,0,'5 6','11','11',NULL,'2026-07-06 18:55:42'),(3,12,1,'ACCEPTED',355,0,'5 6','11','11',NULL,'2026-07-06 18:56:15'),(4,17,1,'ACCEPTED',439,0,'5 6','11','11',NULL,'2026-07-09 02:42:35'),(5,18,1,'ACCEPTED',408,0,'5 6','11','11',NULL,'2026-07-09 02:42:37'),(6,19,1,'WRONG_ANSWER',437,0,'5 6','11','12',NULL,'2026-07-09 02:42:53'),(7,21,1,'WRONG_ANSWER',591,0,'5 6','11','12',NULL,'2026-07-09 02:58:50'),(8,24,1,'ACCEPTED',503,0,'5 6','11','11',NULL,'2026-07-09 13:33:28'),(9,25,1,'ACCEPTED',554,0,'5 6','11','11',NULL,'2026-07-09 13:35:32'),(10,26,1,'ACCEPTED',447,0,'5 6','11','11',NULL,'2026-07-09 13:44:00');
/*!40000 ALTER TABLE `submission_case` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `testcase`
--

DROP TABLE IF EXISTS `testcase`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `testcase` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `problem_id` bigint NOT NULL,
  `input_data` mediumtext NOT NULL,
  `output_data` mediumtext NOT NULL,
  `score` int NOT NULL DEFAULT '100',
  `sort_order` int NOT NULL DEFAULT '0',
  `is_sample` tinyint NOT NULL DEFAULT '0',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_problem_order` (`problem_id`,`sort_order`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `testcase`
--

LOCK TABLES `testcase` WRITE;
/*!40000 ALTER TABLE `testcase` DISABLE KEYS */;
INSERT INTO `testcase` VALUES (1,1,'5 6','11',100,1,0,'2026-07-06 18:54:07');
/*!40000 ALTER TABLE `testcase` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `user`
--

DROP TABLE IF EXISTS `user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `username` varchar(64) NOT NULL,
  `password_hash` varchar(255) NOT NULL,
  `real_name` varchar(64) DEFAULT NULL,
  `nickname` varchar(64) DEFAULT NULL,
  `role` varchar(20) NOT NULL COMMENT 'teacher/student',
  `status` tinyint NOT NULL DEFAULT '1',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  KEY `idx_role` (`role`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `user`
--

LOCK TABLES `user` WRITE;
/*!40000 ALTER TABLE `user` DISABLE KEYS */;
INSERT INTO `user` VALUES (1,'admin','B3C3BIf2Bl4HbdptyCW+Xg==:ik9lIKmLSTwtSRD/qJvdJGvyB71pEMA68qKv7Cohtk0=','谢',NULL,'teacher',1,'2026-07-06 14:51:20','2026-07-06 14:51:20'),(2,'王小明','EZTu5W/E3LO/iW+8FVVxaw==:tF+7vpaK5nxmRxEtJbc2ndejBqEdGmHsQgIAzzSEwuo=','王小明',NULL,'student',1,'2026-07-06 15:00:33','2026-07-06 15:00:33');
/*!40000 ALTER TABLE `user` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping routines for database 'cppkid_oj'
--
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-07-09 14:51:09
