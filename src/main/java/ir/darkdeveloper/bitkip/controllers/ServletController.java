package ir.darkdeveloper.bitkip.controllers;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

import static ir.darkdeveloper.bitkip.config.AppConfigs.log;

public class ServletController extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.getWriter().write("Hello, world!");
        log.info("from servlet get");
    }
}
