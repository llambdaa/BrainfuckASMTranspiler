global _start

section .text
_start:

    ;initialize base pointer (rbp)
    mov     rbp, rsp

    ;set stack frame size and clear
    mov     %STACK_SIZE_REGISTER%, %STACK_SIZE%
.loop_clear:
    cmp     %STACK_SIZE_REGISTER%, 0x0
    je      .exit_clear
    dec     rsp
    dec     %STACK_SIZE_REGISTER%
    mov     byte [rsp], 0x0
    jmp     .loop_clear
.exit_clear:

    ;prepare array index pointer
    mov     %POINTER_REGISTER%, rsp

    ;transpiled brainfuck source
%SOURCE%
    ;exit gracefully
    mov     rax, 0x3C
    mov     rdi, 0x0
    syscall
    ret