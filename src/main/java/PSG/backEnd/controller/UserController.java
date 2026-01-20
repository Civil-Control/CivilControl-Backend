package PSG.backEnd.controller;

import PSG.backEnd.model.dto.security.UserFilterDTO;
import PSG.backEnd.model.dto.security.UserRequestDTO;
import PSG.backEnd.model.dto.security.UserResponseDTO;
import PSG.backEnd.model.dto.security.UserUpdateOwnProfileResponseDTO;
import PSG.backEnd.service.port.IUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

/**
 * REST controller for user management.
 * Allows CRUD operations on system users.
 *
 * IMPORTANT: This controller should be protected with @PreAuthorize in production.
 * Example: @PreAuthorize("hasAuthority('USER_MANAGEMENT')")
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "User Management",
     description = "API for managing system users. " +
                   "Handles user creation, role assignment, password encryption (BCrypt), and user lifecycle. " +
                   "Passwords are never returned in responses.")
public class UserController {

    private final IUserService userService;

    @PostMapping
    @Operation(summary = "Create a new user",
            description = "Creates a new user in the system with assigned roles. " +
                         "Password is automatically encrypted using BCrypt. " +
                         "Username and email must be unique. At least one role is required.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "User successfully created"),
            @ApiResponse(responseCode = "400", description = "Invalid input data or validation error"),
            @ApiResponse(responseCode = "404", description = "One or more role IDs not found"),
            @ApiResponse(responseCode = "409", description = "Username or email already exists")
    })
    public ResponseEntity<UserResponseDTO> createUser(
            @Valid @RequestBody UserRequestDTO requestDTO) {
        UserResponseDTO created = userService.createUser(requestDTO);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();

        return ResponseEntity.created(location).body(created);
    }

    @GetMapping
    @Operation(summary = "Get all users with filters",
            description = "Retrieves a paginated list of users with optional filtering by username, email, name, and enabled status. " +
                         "Supports sorting by any field. Passwords are never included in the response. " +
                         "The currently authenticated user is automatically excluded from the results.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Users retrieved successfully")
    })
    public ResponseEntity<Page<UserResponseDTO>> getAllUsers(
            @Parameter(description = "Filter by username (partial match, case-insensitive)", example = "jgarcia")
            @RequestParam(required = false) String username,

            @Parameter(description = "Filter by email (partial match, case-insensitive)", example = "garcia@esea.com.ar")
            @RequestParam(required = false) String email,

            @Parameter(description = "Filter by first name (partial match, case-insensitive)", example = "Juan")
            @RequestParam(required = false) String firstName,

            @Parameter(description = "Filter by last name (partial match, case-insensitive)", example = "García")
            @RequestParam(required = false) String lastName,

            @Parameter(description = "Filter by enabled status", example = "true")
            @RequestParam(required = false) Boolean enabled,

            @Parameter(description = "Page number (0-indexed)", example = "0")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Page size", example = "10")
            @RequestParam(defaultValue = "10") int size,

            @Parameter(description = "Sort field", example = "firstName")
            @RequestParam(defaultValue = "firstName") String sortBy,

            @Parameter(description = "Sort direction (ASC or DESC)", example = "ASC")
            @RequestParam(defaultValue = "ASC") String sortDir,

            Authentication authentication) {

        Sort sort = sortDir.equalsIgnoreCase("DESC")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(page, size, sort);
        UserFilterDTO filterDTO = new UserFilterDTO(username, email, firstName, lastName, enabled);

        // Get current authenticated username to exclude from results
        String currentUsername = authentication != null ? authentication.getName() : null;
        Page<UserResponseDTO> users = userService.getAllUsersExcludingCurrent(filterDTO, pageable, currentUsername);
        return ResponseEntity.ok(users);
    }

    @PatchMapping("/me")
    @Operation(summary = "Update own profile",
            description = "Updates the authenticated user's own profile. Returns updated user data and new authentication tokens " +
                         "to maintain the session (auto-login). This prevents the user from being logged out after profile changes. " +
                         "Only provided fields will be updated. If password is provided, it will be re-encrypted with BCrypt.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Profile successfully updated with new tokens"),
            @ApiResponse(responseCode = "400", description = "Invalid input data or validation error"),
            @ApiResponse(responseCode = "401", description = "User not authenticated"),
            @ApiResponse(responseCode = "404", description = "User or role not found"),
            @ApiResponse(responseCode = "409", description = "Update would create duplicate username or email")
    })
    public ResponseEntity<UserUpdateOwnProfileResponseDTO> updateOwnProfile(
            @RequestBody UserRequestDTO requestDTO,
            Authentication authentication) {

        String currentUsername = authentication.getName();
        UserUpdateOwnProfileResponseDTO response = userService.updateOwnProfile(currentUsername, requestDTO);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get user by ID",
            description = "Retrieves detailed information about a specific user including all assigned roles. " +
                         "Password is never included in the response.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User found"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<UserResponseDTO> getUserById(
            @Parameter(description = "User unique identifier", required = true, example = "1")
            @PathVariable Long id) {
        UserResponseDTO user = userService.getUserById(id);
        return ResponseEntity.ok(user);
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Update user",
            description = "Updates an existing user. Only provided fields will be updated. " +
                         "If password is provided, it will be re-encrypted with BCrypt. " +
                         "Validates that the user exists and isn't marked as deleted. " +
                         "Cannot update system users (root, admin).")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User successfully updated"),
            @ApiResponse(responseCode = "400", description = "Invalid input data or validation error"),
            @ApiResponse(responseCode = "404", description = "User or role not found"),
            @ApiResponse(responseCode = "409", description = "Update would create duplicate username or email")
    })
    public ResponseEntity<UserResponseDTO> updateUser(
            @Parameter(description = "User unique identifier", required = true, example = "1")
            @PathVariable Long id,

            @RequestBody UserRequestDTO requestDTO) {
        UserResponseDTO updated = userService.updateUser(id, requestDTO);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete user",
            description = "Performs a soft delete of a user from the system. " +
                         "The user is marked as deleted and disabled but remains in the database for historical purposes. " +
                         "Cannot delete system users (root, admin).")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "User successfully deleted"),
            @ApiResponse(responseCode = "400", description = "Cannot delete system user"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<Void> deleteUser(
            @Parameter(description = "User unique identifier", required = true, example = "1")
            @PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}
