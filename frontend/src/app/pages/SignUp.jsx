import { useState } from "react"
import { } from "reactstrap-formik";
import { Form, Field, Formik, ErrorMessage } from "formik";
import { Card, CardHeader, CardBody, Col, Container, Row, CardColumns, CardFooter } from "reactstrap";
import Service from "../service/Service"

export default function SignUp() {

    const [user, setUser] = useState({
        username: '',
        firstname: '',
        lastname: '',
        email: '',
        password: '',
        avatar: 'default.jpg',
    })

    const onSubmit = (values) => {
        let user = {
            username: values.username,
            firstname: values.firstname,
            lastname: values.lastname,
            email: values.email,
            password: values.password,
        }
        Service.signup(user);
    }

    const validate = (values) => {

    }

    let { username, firstname, lastname, email, password } = user;
    return (
        <div>
            <Col>
                <Card>
                    <CardHeader>
                        <h3>Đăng ký</h3>
                    </CardHeader>
                    <CardBody>
                        <Formik
                            initialValues={{ username, firstname, lastname, email, password }}
                            onSubmit={onSubmit}
                            validateOnChange={false}
                            validateOnBlur={false}
                            validate={validate}
                            enableReinitialize={true}
                        >
                            {
                                () => (
                                    <Form method="post">
                                        <ErrorMessage name="password" component="div"
                                            className="alert alert-warning" />
                                        <fieldset className="form-group">
                                            <label htmlFor="username" >Tên đăng nhập </label>
                                            <Field className="form-control" type="text" name="username" id="username" />
                                        </fieldset>
                                        <fieldset className="form-group ">
                                            <label>Tên</label>
                                            <Field className="form-control" type="text" name="firstname" />
                                        </fieldset>
                                        <fieldset className="form-group ">
                                            <label>Họ</label>
                                            <Field className="form-control" type="text" name="lastname" />
                                        </fieldset>
                                        <fieldset className="form-group ">
                                            <label>Email</label>
                                            <Field className="form-control" type="text" name="email" />
                                        </fieldset>
                                        <fieldset className="form-group ">
                                            <label>Mật khẩu</label>
                                            <Field className="form-control" type="password" name="password" />
                                        </fieldset>
                                        <fieldset className="form-group ">
                                            <label>Nhập lại mật khẩu</label>
                                            <Field className="form-control" type="password" name="re-attemppassword" />
                                        </fieldset>
                                        <button className="btn btn-success" type="submit">Đăng ký</button>
                                        <button className="btn btn-danger" type="reset">Huỷ</button>
                                    </Form>
                                )
                            }
                        </Formik>
                    </CardBody>
                    <CardFooter>
                        <p>Đã có tài khoản? <a href="/login">Đăng nhập</a></p>
                    </CardFooter>
                </Card>
            </Col>
        </div>
    )
}