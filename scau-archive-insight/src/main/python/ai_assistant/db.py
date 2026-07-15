# -*- coding: utf-8 -*-
import os
import psycopg2
from psycopg2 import pool

DB_CONFIG = {
    "host": os.getenv("DB_HOST", "localhost"),
    "port": int(os.getenv("DB_PORT", "5432")),
    "dbname": os.getenv("DB_NAME", "scau_archive"),
    "user": os.getenv("DB_USER", "postgres"),
    "password": os.getenv("DB_PASS", "123456"),
}

_connection_pool = None


def get_conn():
    global _connection_pool
    if _connection_pool is None:
        _connection_pool = pool.ThreadedConnectionPool(1, 5, **DB_CONFIG)
    return _connection_pool.getconn()


def put_conn(conn):
    global _connection_pool
    if _connection_pool and conn:
        _connection_pool.putconn(conn)
