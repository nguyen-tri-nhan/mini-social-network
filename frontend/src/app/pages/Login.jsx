import { useState } from 'react';
import { Card, CardHeader, CardBody, Col, Row, CardFooter } from 'reactstrap';
import MAuth from '../model/MAuth'
import { Formik, Form, Field } from "formik";
import { useHistory } from 'react-router-dom';

export default function Login() {

    const history = useHistory();

    const [user, setUser] = useState({
        usernameOrEmail: '',
        password: '',
    })

    const onSubmit = (values) => {
        MAuth.login(values)
        .then(() => MAuth.isLoggedIn() && history.push("/"));
    }

    let { usernameOrEmail, password } = user;
    return (
        <div>
            <Row>
                <Col>
                    <Card>
                        <CardHeader>
                            <h3>Đăng nhập</h3>
                        </CardHeader>
                        <CardBody>
                            <Formik
                                initialValues={{ usernameOrEmail, password }}
                                onSubmit={onSubmit}
                                validateOnChange={false}
                                validateOnBlur={false}
                                enableReinitialize={true}
                            >
                                <Form method="post">
                                    <fieldset className="form-group">
                                        <label htmlFor="usernameOrEmail" >Tên đăng nhập hoặc email </label>
                                        <Field className="form-control" type="text" name="usernameOrEmail" id="usernameOrEmail" />
                                    </fieldset>
                                    <fieldset className="form-group ">
                                        <label>Mật khẩu</label>
                                        <Field className="form-control" type="password" name="password" />
                                    </fieldset>
                                    <button className="btn btn-primary" type="submit">Đăng nhập</button>
                                </Form>
                            </Formik>
                        </CardBody>
                        <CardFooter>
                            <p>Chưa có tài khoản? <a href="/signup">Đăng ký ngay!</a></p>
                        </CardFooter>
                    </Card>
                </Col>
            </Row>
        </div>
    );
}