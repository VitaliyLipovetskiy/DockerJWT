package com.example.dockerjwt.user.dto;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class UserTo {
    private String email;
    private String id;
}
